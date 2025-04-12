package com.lb.im.sdk.interfaces.sender.impl;

import com.lb.im.common.cache.distribute.DistributedCacheService;
import com.lb.im.common.domain.constans.IMConstants;
import com.lb.im.common.domain.enums.IMCmdType;
import com.lb.im.common.domain.enums.IMListenerType;
import com.lb.im.common.domain.enums.IMSendCode;
import com.lb.im.common.domain.model.IMPrivateMessage;
import com.lb.im.common.domain.model.IMReceiveInfo;
import com.lb.im.common.domain.model.IMSendResult;
import com.lb.im.common.domain.model.IMUserInfo;
import com.lb.im.common.mq.MessageSenderService;
import com.lb.im.sdk.infrastructure.multicaster.MessageListenerMulticaster;
import com.lb.im.sdk.interfaces.sender.IMSender;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

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
     * @param message 需要发送的私有消息对象
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
     * @param message 需要发送的私有消息对象
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
}
