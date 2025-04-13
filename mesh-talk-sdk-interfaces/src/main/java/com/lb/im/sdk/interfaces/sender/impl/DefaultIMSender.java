package com.lb.im.sdk.interfaces.sender.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.lb.im.common.cache.distribute.DistributedCacheService;
import com.lb.im.common.domain.constans.IMConstants;
import com.lb.im.common.domain.enums.IMCmdType;
import com.lb.im.common.domain.enums.IMDeviceType;
import com.lb.im.common.domain.enums.IMListenerType;
import com.lb.im.common.domain.enums.IMSendCode;
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
 * 默认的即时消息发送实现类，负责处理私有消息的发送逻辑
 * 依赖分布式缓存服务、消息队列服务和消息监听器多播器实现消息分发
 */
@Service
public class DefaultIMSender implements IMSender {

    @Autowired
    private DistributedCacheService distributedCacheService;

    @Autowired
    private MessageSenderService messageSenderService;

    @Autowired
    private MessageListenerMulticaster messageListenerMulticaster;

    /**
     * 发送私有消息到目标用户及自身设备
     *
     * @param message 需要发送的私有消息对象，包含发送者、接收者、内容及设备类型等信息
     */
    @Override
    public <T> void sendPrivateMessage(IMPrivateMessage<T> message) {
        if (message == null) return;
        List<Integer> receiveDeviceTypes = message.getReceiveDeviceTypes();
        if (!CollectionUtils.isEmpty(receiveDeviceTypes)) {
            this.sendPrivateMessageToTargetUsers(message, receiveDeviceTypes);
            this.sendPrivateMessageToSelf(message, receiveDeviceTypes);
        }
    }

    /**
     * 向自己的其他终端发送消息
     *
     * @param message            需要发送的私有消息对象
     * @param receiveDeviceTypes 需要接收消息的设备类型列表
     */
    private <T> void sendPrivateMessageToSelf(IMPrivateMessage<T> message, List<Integer> receiveDeviceTypes) {
        if (BooleanUtils.isTrue(message.getSendToself())) {
            receiveDeviceTypes.forEach(receiveDeviceType -> {
                String redisKey = String.join(IMConstants.REDIS_KEY_SPLIT, IMConstants.IM_USER_SERVER_ID, message.getFromUser().getUserId().toString(), receiveDeviceType.toString());
                String serverId = distributedCacheService.get(redisKey);
                if (!StringUtils.isEmpty(serverId)) {
                    String sendKey = String.join(IMConstants.MESSAGE_KEY_SPLIT, IMConstants.IM_MESSAGE_PRIVATE_QUEUE, serverId);
                    IMReceiveInfo imReceivenfo = new IMReceiveInfo(IMCmdType.PRIVATE_MESSAGE.getCode(), message.getFromUser(), Collections.singletonList(new IMUserInfo(message.getFromUser().getUserId(), receiveDeviceType)), false, message.getContent());
                    imReceivenfo.setDestination(sendKey);
                    messageSenderService.send(imReceivenfo);
                }
            });
        }
    }

    /**
     * 向目标用户发送私有消息
     *
     * @param message            需要发送的私有消息对象
     * @param receiveDeviceTypes 需要接收消息的设备类型列表
     */
    private <T> void sendPrivateMessageToTargetUsers(IMPrivateMessage<T> message, List<Integer> receiveDeviceTypes) {
        receiveDeviceTypes.forEach(receiveDeviceType -> {
            // 获取接收用户对应服务器ID
            String redisKey = String.join(IMConstants.REDIS_KEY_SPLIT, IMConstants.IM_USER_SERVER_ID, message.getReceiverId().toString(), receiveDeviceType.toString());
            String serverId = distributedCacheService.get(redisKey);

            // 用户在线时推送消息到消息队列
            if (!StringUtils.isEmpty(serverId)) {
                String sendKey = String.join(IMConstants.MESSAGE_KEY_SPLIT, IMConstants.IM_MESSAGE_PRIVATE_QUEUE, serverId);
                IMReceiveInfo imReceivenfo = new IMReceiveInfo(IMCmdType.PRIVATE_MESSAGE.getCode(), message.getFromUser(), Collections.singletonList(new IMUserInfo(message.getReceiverId(), receiveDeviceType)), message.getSendResult(), message.getContent());
                imReceivenfo.setDestination(sendKey);
                messageSenderService.send(imReceivenfo);
            } else if (message.getSendResult()) {
                // 用户不在线时发送状态回执
                IMSendResult<T> result = new IMSendResult<>(message.getFromUser(), new IMUserInfo(message.getReceiverId(), receiveDeviceType), IMSendCode.NOT_ONLINE.code(), message.getContent());
                messageListenerMulticaster.multicast(IMListenerType.PRIVATE_MESSAGE, result);
            }
        });
    }


    @Override
    public <T> void sendGroupMessage(IMGroupMessage<T> message) {
        Map<String, IMUserInfo> userTerminalGroup = this.getUserTerminalGroup(message);
        //分组数据为空，直接返回
        if (CollectionUtil.isEmpty(userTerminalGroup)) {
            return;
        }
        //从Redis批量拉取数据
        List<String> serverIdList = distributedCacheService.multiGet(userTerminalGroup.keySet());
        if (CollectionUtil.isEmpty(serverIdList)) {
            return;
        }
        //将接收方按照服务Id进行分组，Key-服务ID，Value-接收消息的用户列表
        Map<Integer, List<IMUserInfo>> serverMap = new HashMap<>();
        //离线用户列表
        List<IMUserInfo> offlineUserList = new LinkedList<>();
        int idx = 0;
        for (Map.Entry<String, IMUserInfo> entry : userTerminalGroup.entrySet()) {
            String serverIdStr = serverIdList.get(idx++);
            if (StringUtils.isEmpty(serverIdStr)) {
                List<IMUserInfo> list = serverMap.computeIfAbsent(Integer.parseInt(serverIdStr), o -> new LinkedList<>());
                list.add(entry.getValue());
            } else {
                offlineUserList.add(entry.getValue());
            }
        }
        //向群组其他成员发送消息
        this.sendGroupMessageToOtherUsers(serverMap, offlineUserList, message);
        //推送给自己的其他终端
        this.sendGroupMessageToSelf(message);
    }

    /**
     * 推送给自己的其他终端
     */
    private <T> void sendGroupMessageToSelf(IMGroupMessage<T> message) {
        for (Integer terminal : IMDeviceType.getAllCode()) {
            //向不是发消息的终端推送消息
            if (!terminal.equals(message.getFromUser().getDeviceType())) {
                // 获取连接终端的channelId
                String redisKey = String.join(IMConstants.REDIS_KEY_SPLIT, IMConstants.IM_USER_SERVER_ID, message.getFromUser().getUserId().toString(), terminal.toString());
                String serverId = distributedCacheService.get(redisKey);
                if (!StrUtil.isEmpty(serverId)) {
                    IMReceiveInfo imReceiveInfo = new IMReceiveInfo(IMCmdType.GROUP_MESSAGE.getCode(), message.getFromUser(), Collections.singletonList(new IMUserInfo(message.getFromUser().getUserId(), terminal)), false, message.getContent());
                    String sendKey = String.join(IMConstants.MESSAGE_KEY_SPLIT, IMConstants.IM_MESSAGE_GROUP_QUEUE, serverId);
                    imReceiveInfo.setDestination(sendKey);
                    messageSenderService.send(imReceiveInfo);
                }
            }
        }
    }

    /**
     * 向群组其他成员发送消息
     */
    private <T> void sendGroupMessageToOtherUsers(Map<Integer, List<IMUserInfo>> serverMap, List<IMUserInfo> offlineUserList, IMGroupMessage<T> message) {
        // 遍历服务器分组列表，为每个服务器构建消息载体并发送到对应队列
        for (Map.Entry<Integer, List<IMUserInfo>> entry : serverMap.entrySet()) {
            IMReceiveInfo imReceiveInfo = new IMReceiveInfo(IMCmdType.GROUP_MESSAGE.getCode(), message.getFromUser(), new LinkedList<>(entry.getValue()), message.getSendResult(), message.getContent());
            String sendKey = String.join(IMConstants.MESSAGE_KEY_SPLIT, IMConstants.IM_MESSAGE_GROUP_QUEUE, entry.getKey().toString());
            imReceiveInfo.setDestination(sendKey);
            messageSenderService.send(imReceiveInfo);
        }
        // 若消息发送成功，为每个离线用户生成未在线状态结果并通过监听器广播
        if (message.getSendResult()) {
            offlineUserList.forEach((offlineUser) -> {
                IMSendResult<T> result = new IMSendResult<>(message.getFromUser(), offlineUser, IMSendCode.NOT_ONLINE.code(), message.getContent());
                messageListenerMulticaster.multicast(IMListenerType.GROUP_MESSAGE, result);
            });
        }
    }

    private <T> Map<String, IMUserInfo> getUserTerminalGroup(IMGroupMessage<T> message) {
        Map<String, IMUserInfo> map = new HashMap<>();
        if (message == null) {
            return map;
        }
        for (Integer terminal : message.getReceiveDeviceTypes()) {
            message.getReceiverIds().forEach((receiveId) -> {
                String key = String.join(IMConstants.REDIS_KEY_SPLIT, IMConstants.IM_USER_SERVER_ID, receiveId.toString(), terminal.toString());
                map.put(key, new IMUserInfo(receiveId, terminal));
            });
        }
        return map;
    }

    @Override
    /**
     * 根据用户ID列表查询在线终端类型
     * @param userIds 需要查询的用户ID列表
     * @return 每个用户对应的在线终端类型列表，键为用户ID，值为终端类型列表
     */
    public Map<Long, List<IMDeviceType>> getOnlineTerminal(List<Long> userIds) {
        if (CollectionUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }

        // 构建用户与终端类型的键值对，用于Redis查询
        Map<String, IMUserInfo> userMap = new HashMap<>();
        for (Long userId : userIds) {
            for (Integer terminal : IMDeviceType.getAllCode()) {
                String key = String.join(IMConstants.REDIS_KEY_SPLIT, IMConstants.IM_USER_SERVER_ID, userId.toString(), terminal.toString());
                userMap.put(key, new IMUserInfo(userId, terminal));
            }
        }

        // 从Redis批量获取数据
        List<String> serverIdList = distributedCacheService.multiGet(userMap.keySet());
        int idx = 0;
        Map<Long, List<IMDeviceType>> onlineMap = new HashMap<>();
        for (Map.Entry<String, IMUserInfo> entry : userMap.entrySet()) {
            if (!StrUtil.isEmpty(serverIdList.get(idx++))) {
                IMUserInfo imUserInfo = entry.getValue();
                List<IMDeviceType> imTerminalTypeList = onlineMap.computeIfAbsent(imUserInfo.getUserId(), o -> new LinkedList<>());
                imTerminalTypeList.add(IMDeviceType.getByCode(imUserInfo.getDeviceType()));
            }
        }
        return onlineMap;
    }


    @Override
    public Boolean isOnline(Long userId) {
        String redisKey = String.join(IMConstants.REDIS_KEY_SPLIT, IMConstants.IM_USER_SERVER_ID, userId.toString(), "*");
        Set<String> keys = distributedCacheService.keys(redisKey);
        return !CollectionUtil.isEmpty(keys);
    }

    @Override
    public List<Long> getOnlineUser(List<Long> userIds) {
        return new LinkedList<>(this.getOnlineTerminal(userIds).keySet());
    }
}
