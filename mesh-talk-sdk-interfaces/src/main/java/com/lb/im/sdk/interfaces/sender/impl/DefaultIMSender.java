package com.lb.im.sdk.interfaces.sender.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.lb.im.common.cache.distribute.DistributedCacheService;
import com.lb.im.common.domain.constans.IMConstants;
import com.lb.im.common.domain.enums.IMCmdType;
import com.lb.im.common.domain.enums.IMListenerType;
import com.lb.im.common.domain.enums.IMSendCode;
import com.lb.im.common.domain.enums.IMTerminalType;
import com.lb.im.common.domain.model.*;
import com.lb.im.common.mq.MessageSenderService;
import com.lb.im.sdk.infrastructure.multicaster.MessageListenerMulticaster;
import com.lb.im.sdk.interfaces.sender.IMSender;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 默认的即时消息发送实现类
 * <p>
 * 该类负责处理即时通讯系统中的消息发送逻辑，包括私聊消息和群聊消息的处理。
 * 它采用分布式架构设计，支持跨服务器消息路由和多终端同步。
 * <p>
 * 设计要点：
 * 1. 使用Redis作为分布式缓存 - 存储用户在线状态和所连接的服务器ID，便于快速查询和路由
 * 2. 采用消息队列实现异步通信 - 提高系统吞吐量，降低耦合度
 * 3. 多终端消息同步 - 支持用户在多个设备上接收相同消息
 * 4. 离线用户处理 - 提供离线状态反馈机制
 * <p>
 * 依赖组件：
 * 1. 分布式缓存服务(DistributedCacheService) - 基于Redis实现用户状态和路由信息存储
 * 2. 消息队列服务(MessageSenderService) - 用于异步发送消息到目标服务器
 * 3. 消息监听器多播器(MessageListenerMulticaster) - 用于广播消息发送结果给相关监听器
 */
@Service
public class DefaultIMSender implements IMSender {

    /**
     * 分布式缓存服务，基于Redis实现
     * 主要用途：
     * 1. 存储用户终端与服务器的映射关系
     * 2. 查询用户在线状态
     * 3. 为消息路由提供服务器ID信息
     */
    @Autowired
    private DistributedCacheService distributedCacheService;

    /**
     * 消息发送服务，基于消息队列实现
     * 主要用途：
     * 1. 将消息异步发送到特定的消息队列
     * 2. 根据服务器ID路由消息到正确的目标服务器
     * <p>
     * 设计说明：使用消息队列而非直接HTTP调用，可以提高系统吞吐量，
     * 并在目标服务不可用时提供消息缓冲，增强系统可靠性
     */
    @Autowired
    private MessageSenderService messageSenderService;

    /**
     * 消息监听器多播器
     * 主要用途：
     * 1. 向注册的监听器广播消息发送结果
     * 2. 特别用于通知消息发送失败或用户不在线等状态
     * <p>
     * 设计说明：使用观察者模式，使得消息发送结果可以被多个组件感知，
     * 便于上层应用实现更复杂的业务逻辑
     */
    @Autowired
    private MessageListenerMulticaster messageListenerMulticaster;

    /**
     * 发送私有消息到目标用户及自身其他设备
     * <p>
     * 流程：
     * 1. 验证消息有效性
     * 2. 向目标用户发送消息
     * 3. 向发送者自己的其他终端同步消息（如果需要）
     *
     * @param message 需要发送的私有消息对象，包含发送者、接收者、内容及设备类型等信息
     */
    @Override
    public <T> void sendPrivateMessage(IMPrivateMessage<T> message) {
        // 空消息检查，防止NPE
        if (message == null) return;

        // 获取接收终端类型列表
        List<Integer> receiveDeviceTypes = message.getReceiveTerminals();

        // 确保至少有一个接收终端类型
        if (!CollectionUtils.isEmpty(receiveDeviceTypes)) {
            // 向目标用户发送消息
            this.sendPrivateMessageToTargetUsers(message, receiveDeviceTypes);

            // 消息同步到发送者自己的其他设备
            this.sendPrivateMessageToSelf(message, receiveDeviceTypes);
        }
    }

    /**
     * 向自己的其他终端发送消息（消息同步功能）
     * <p>
     * 设计说明：
     * 1. 多终端同步是现代IM系统的基本需求，确保用户在所有设备上看到一致的消息记录
     * 2. 仅在用户明确要求同步时才执行(通过sendToSelf标志控制)
     * 3. 采用与普通消息发送相同的异步队列机制，但标记为非需要回执(setSendResult=false)
     *
     * @param message            需要发送的私有消息对象
     * @param receiveDeviceTypes 需要接收消息的设备类型列表
     */
    private <T> void sendPrivateMessageToSelf(IMPrivateMessage<T> message, List<Integer> receiveDeviceTypes) {
        // 只有当sendToSelf标志为true时才执行同步
        if (BooleanUtils.isTrue(message.getSendToSelf())) {
            // 遍历所有接收终端类型
            receiveDeviceTypes.forEach(receiveDeviceType -> {
                // 构建Redis键，格式：IM_USER_SERVER_ID:userId:terminalType
                // 这个键用于查询用户特定终端连接的服务器ID
                String redisKey = String.join(IMConstants.REDIS_KEY_SPLIT, IMConstants.IM_USER_SERVER_ID, message.getSender().getUserId().toString(), receiveDeviceType.toString());

                // 从Redis获取该终端连接的服务器ID
                String serverId = distributedCacheService.get(redisKey);

                // 只有当服务器ID存在（用户该终端在线）时才发送
                if (!StringUtils.isEmpty(serverId)) {
                    // 构建消息队列的目标键，格式：IM_MESSAGE_PRIVATE_QUEUE:serverId
                    String sendKey = String.join(IMConstants.MESSAGE_KEY_SPLIT, IMConstants.IM_MESSAGE_PRIVATE_QUEUE, serverId);

                    // 创建接收信息对象
                    // 注意：发送给自己时不需要回执，所以setSendResult=false
                    IMReceiveInfo imReceivenfo = new IMReceiveInfo(
                            IMCmdType.PRIVATE_MESSAGE.getCode(),
                            message.getSender(),
                            Collections.singletonList(new IMUserInfo(message.getSender().getUserId(), receiveDeviceType)),
                            false,  // 不需要发送结果回执
                            message.getData());

                    // 设置消息目标队列
                    imReceivenfo.setDestination(sendKey);

                    // 通过消息队列异步发送
                    messageSenderService.send(imReceivenfo);
                }
            });
        }
    }

    /**
     * 向目标用户发送私有消息
     * <p>
     * 设计说明：
     * 1. 基于Redis快速查询目标用户的在线状态和所连接的服务器
     * 2. 对在线用户，通过消息队列将消息路由到对应服务器
     * 3. 对离线用户，通过监听器机制通知发送者消息未送达
     * 4. 支持向用户的多个终端类型发送消息
     *
     * @param message            需要发送的私有消息对象
     * @param receiveDeviceTypes 需要接收消息的设备类型列表
     */
    private <T> void sendPrivateMessageToTargetUsers(IMPrivateMessage<T> message, List<Integer> receiveDeviceTypes) {
        // 遍历所有目标终端类型
        receiveDeviceTypes.forEach(receiveDeviceType -> {
            // 构建Redis键，查询接收用户特定终端连接的服务器ID
            String redisKey = String.join(IMConstants.REDIS_KEY_SPLIT, IMConstants.IM_USER_SERVER_ID, message.getReceiveId().toString(), receiveDeviceType.toString());

            // 获取服务器ID
            String serverId = distributedCacheService.get(redisKey);

            // 如果服务器ID存在，表示用户在线
            if (!StringUtils.isEmpty(serverId)) {
                // 构建消息队列目标键
                String sendKey = String.join(IMConstants.MESSAGE_KEY_SPLIT, IMConstants.IM_MESSAGE_PRIVATE_QUEUE, serverId);

                // 创建消息接收信息对象
                IMReceiveInfo imReceivenfo = new IMReceiveInfo(
                        IMCmdType.PRIVATE_MESSAGE.getCode(),
                        message.getSender(),
                        Collections.singletonList(new IMUserInfo(message.getReceiveId(), receiveDeviceType)),
                        message.getSendResult(),  // 是否需要发送结果回执
                        message.getData());

                // 设置消息目标队列
                imReceivenfo.setDestination(sendKey);

                // 通过消息队列异步发送
                messageSenderService.send(imReceivenfo);
            }
            // 用户不在线且需要发送结果回执
            else if (message.getSendResult()) {
                // 创建发送结果对象，状态为"用户不在线"
                IMSendResult<T> result = new IMSendResult<>(
                        message.getSender(),
                        new IMUserInfo(message.getReceiveId(), receiveDeviceType),
                        IMSendCode.NOT_ONLINE.getCode(),  // 不在线状态码
                        message.getData());

                // 通过监听器多播器通知消息未送达
                // 这允许应用程序对未送达消息做进一步处理，如离线存储等
                messageListenerMulticaster.multicast(IMListenerType.PRIVATE_MESSAGE, result);
            }
        });
    }

    /**
     * 发送群聊消息
     * <p>
     * 设计说明：
     * 1. 群聊消息需要发送给多个接收者，直接发送可能导致大量冗余网络请求
     * 2. 采用先按服务器分组，再批量发送的策略，减少网络请求次数
     * 3. 区分在线用户和离线用户，分别处理
     * 4. 同样支持向发送者自己的其他终端同步消息
     *
     * @param message 群组消息对象
     */
    @Override
    public <T> void sendGroupMessage(IMGroupMessage<T> message) {
        // 获取用户-终端分组映射，用于批量查询在线状态
        Map<String, IMUserInfo> userTerminalGroup = this.getUserTerminalGroup(message);

        // 如果分组数据为空，直接返回
        if (CollectionUtil.isEmpty(userTerminalGroup)) {
            return;
        }

        // 从Redis批量获取服务器ID信息，提高查询效率
        // 这比循环单个查询Redis性能要好得多
        List<String> serverIdList = distributedCacheService.multiGet(userTerminalGroup.keySet());
        if (CollectionUtil.isEmpty(serverIdList)) {
            return;
        }

        // 将接收方按照服务ID进行分组，便于批量发送
        // 键为服务器ID，值为接收消息的用户列表
        Map<Integer, List<IMUserInfo>> serverMap = new HashMap<>();

        // 离线用户列表，用于后续发送离线通知
        List<IMUserInfo> offlineUserList = new LinkedList<>();

        // 遍历用户终端映射，根据服务器ID进行分组
        int idx = 0;
        for (Map.Entry<String, IMUserInfo> entry : userTerminalGroup.entrySet()) {
            String serverIdStr = serverIdList.get(idx++);
            if (!StringUtils.isEmpty(serverIdStr)) {
                // 用户在线，加入对应服务器的用户列表
                List<IMUserInfo> list = serverMap.computeIfAbsent(
                        Integer.parseInt(serverIdStr),
                        o -> new LinkedList<>());
                list.add(entry.getValue());
            } else {
                // 用户不在线，加入离线用户列表
                offlineUserList.add(entry.getValue());
            }
        }

        // 向群组其他成员发送消息
        this.sendGroupMessageToOtherUsers(serverMap, offlineUserList, message);

        // 向发送者自己的其他终端同步消息
        this.sendGroupMessageToSelf(message);
    }

    /**
     * 推送群聊消息给自己的其他终端
     * <p>
     * 设计说明：
     * 1. 与私聊消息不同，群聊消息默认会同步到自己的所有其他终端
     * 2. 排除发送消息的当前终端，避免消息重复
     *
     * @param message 群聊消息对象
     */
    private <T> void sendGroupMessageToSelf(IMGroupMessage<T> message) {
        // 遍历所有终端类型
        for (Integer terminal : IMTerminalType.getAllCode()) {
            // 排除发送消息的当前终端，避免重复接收
            if (!terminal.equals(message.getSender().getTerminal())) {
                // 构建Redis键，查询该终端连接的服务器ID
                String redisKey = String.join(IMConstants.REDIS_KEY_SPLIT,
                                              IMConstants.IM_USER_SERVER_ID,
                                              message.getSender().getUserId().toString(),
                                              terminal.toString());

                // 获取服务器ID
                String serverId = distributedCacheService.get(redisKey);

                // 如果服务器ID存在（即该终端在线），发送消息
                if (!StrUtil.isEmpty(serverId)) {
                    // 创建消息接收信息对象
                    IMReceiveInfo imReceiveInfo = new IMReceiveInfo(
                            IMCmdType.GROUP_MESSAGE.getCode(),
                            message.getSender(),
                            Collections.singletonList(new IMUserInfo(message.getSender().getUserId(), terminal)),
                            false,  // 不需要发送结果回执
                            message.getData());

                    // 构建消息队列目标键
                    String sendKey = String.join(IMConstants.MESSAGE_KEY_SPLIT,
                                                 IMConstants.IM_MESSAGE_GROUP_QUEUE,
                                                 serverId);

                    // 设置消息目标队列
                    imReceiveInfo.setDestination(sendKey);

                    // 通过消息队列异步发送
                    messageSenderService.send(imReceiveInfo);
                }
            }
        }
    }

    /**
     * 向群组其他成员发送消息
     * <p>
     * 设计说明：
     * 1. 按服务器分组批量发送，减少网络请求次数
     * 2. 对离线用户生成未在线状态通知
     * 3. 利用消息队列实现异步发送，提高系统吞吐量
     *
     * @param serverMap       服务器ID到用户列表的映射
     * @param offlineUserList 离线用户列表
     * @param message         群聊消息对象
     */
    private <T> void sendGroupMessageToOtherUsers(Map<Integer, List<IMUserInfo>> serverMap, List<IMUserInfo> offlineUserList, IMGroupMessage<T> message) {
        // 遍历服务器分组，为每个服务器构建一条包含多个接收者的消息
        for (Map.Entry<Integer, List<IMUserInfo>> entry : serverMap.entrySet()) {
            // 创建消息接收信息对象，一个消息对象包含了同一服务器上的多个接收者
            IMReceiveInfo imReceiveInfo = new IMReceiveInfo(
                    IMCmdType.GROUP_MESSAGE.getCode(),
                    message.getSender(),
                    new LinkedList<>(entry.getValue()),  // 该服务器上的接收用户列表
                    message.getSendResult(),  // 是否需要发送结果回执
                    message.getData());

            // 构建消息队列目标键
            String sendKey = String.join(IMConstants.MESSAGE_KEY_SPLIT,
                                         IMConstants.IM_MESSAGE_GROUP_QUEUE,
                                         entry.getKey().toString());

            // 设置消息目标队列
            imReceiveInfo.setDestination(sendKey);

            // 通过消息队列异步发送
            messageSenderService.send(imReceiveInfo);
        }

        // 如果需要发送结果回执，则处理离线用户
        if (message.getSendResult()) {
            // 为每个离线用户生成未在线状态通知
            offlineUserList.forEach((offlineUser) -> {
                // 创建发送结果对象，状态为"用户不在线"
                IMSendResult<T> result = new IMSendResult<>(
                        message.getSender(),
                        offlineUser,
                        IMSendCode.NOT_ONLINE.getCode(),  // 不在线状态码
                        message.getData());

                // 通过监听器多播器通知消息未送达
                messageListenerMulticaster.multicast(IMListenerType.GROUP_MESSAGE, result);
            });
        }
    }

    /**
     * 构建用户终端分组，用于批量查询用户在线状态
     * <p>
     * 设计说明：
     * 1. 将用户ID和终端类型组合成Redis键，便于批量查询
     * 2. 返回键值对映射，键为Redis查询键，值为用户信息对象
     *
     * @param message 群聊消息对象
     * @return 用户终端分组映射
     */
    private <T> Map<String, IMUserInfo> getUserTerminalGroup(IMGroupMessage<T> message) {
        Map<String, IMUserInfo> map = new HashMap<>();
        if (message == null) {
            return map;
        }

        // 遍历所有接收终端类型
        for (Integer terminal : message.getReceiveTerminals()) {
            // 遍历所有接收用户ID
            message.getReceiveIds().forEach((receiveId) -> {
                // 构建Redis键，格式：IM_USER_SERVER_ID:userId:terminalType
                String key = String.join(IMConstants.REDIS_KEY_SPLIT,
                                         IMConstants.IM_USER_SERVER_ID,
                                         receiveId.toString(),
                                         terminal.toString());

                // 将键与对应的用户信息放入映射
                map.put(key, new IMUserInfo(receiveId, terminal));
            });
        }
        return map;
    }

    /**
     * 根据用户ID列表查询在线终端类型
     * <p>
     * 设计说明：
     * 1. 批量查询提高效率，避免多次Redis网络往返
     * 2. 支持查询多个用户的多个终端类型
     * 3. 返回用户ID到终端类型列表的映射，便于上层应用使用
     *
     * @param userIds 需要查询的用户ID列表
     * @return 每个用户对应的在线终端类型列表，键为用户ID，值为终端类型列表
     */
    @Override
    public Map<Long, List<IMTerminalType>> getOnlineTerminal(List<Long> userIds) {
        // 空列表检查
        if (CollectionUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }

        // 构建用户与终端类型的键值对，用于Redis批量查询
        Map<String, IMUserInfo> userMap = new HashMap<>();
        for (Long userId : userIds) {
            // 为每个用户的每种终端类型创建一个查询键
            for (Integer terminal : IMTerminalType.getAllCode()) {
                String key = String.join(IMConstants.REDIS_KEY_SPLIT,
                                         IMConstants.IM_USER_SERVER_ID,
                                         userId.toString(),
                                         terminal.toString());
                userMap.put(key, new IMUserInfo(userId, terminal));
            }
        }

        // 从Redis批量获取数据，一次网络请求获取所有用户终端的在线状态
        List<String> serverIdList = distributedCacheService.multiGet(userMap.keySet());

        // 处理查询结果，构建返回映射
        int idx = 0;
        Map<Long, List<IMTerminalType>> onlineMap = new HashMap<>();
        for (Map.Entry<String, IMUserInfo> entry : userMap.entrySet()) {
            // 如果服务器ID存在（不为空），表示该终端在线
            if (!StrUtil.isEmpty(serverIdList.get(idx++))) {
                IMUserInfo imUserInfo = entry.getValue();

                // 获取或创建用户的终端类型列表
                List<IMTerminalType> imTerminalTypeList = onlineMap.computeIfAbsent(
                        imUserInfo.getUserId(),
                        o -> new LinkedList<>());

                // 添加终端类型到列表
                imTerminalTypeList.add(IMTerminalType.getByCode(imUserInfo.getTerminal()));
            }
        }
        return onlineMap;
    }

    /**
     * 判断用户是否在线
     * <p>
     * 设计说明：
     * 1. 使用Redis的模式匹配查询，一次查询获取用户所有终端的在线状态
     * 2. 如果任一终端在线，则视为用户在线
     *
     * @param userId 用户ID
     * @return 用户是否在线
     */
    @Override
    public Boolean isOnline(Long userId) {
        // 构建Redis模式匹配键，使用通配符匹配所有终端
        String redisKey = String.join(IMConstants.REDIS_KEY_SPLIT,
                                      IMConstants.IM_USER_SERVER_ID,
                                      userId.toString(),
                                      "*");

        // 获取匹配的所有键
        Set<String> keys = distributedCacheService.keys(redisKey);

        // 如果存在匹配的键，表示用户至少有一个终端在线
        return !CollectionUtil.isEmpty(keys);
    }

    /**
     * 获取在线用户列表
     * <p>
     * 设计说明：
     * 1. 复用getOnlineTerminal方法，避免重复代码
     * 2. 只返回用户ID列表，不关心具体终端类型
     *
     * @param userIds 需要查询的用户ID列表
     * @return 在线用户ID列表
     */
    @Override
    public List<Long> getOnlineUser(List<Long> userIds) {
        // 调用getOnlineTerminal方法获取在线用户映射，然后只取用户ID集合
        return new LinkedList<>(this.getOnlineTerminal(userIds).keySet());
    }
}