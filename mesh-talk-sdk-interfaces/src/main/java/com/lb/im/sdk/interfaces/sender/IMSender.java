package com.lb.im.sdk.interfaces.sender;

import com.lb.im.common.domain.model.IMGroupMessage;
import com.lb.im.common.domain.model.IMPrivateMessage;
import com.lb.im.common.domain.model.IMSendResult;

/**
 * 消息发送接口
 */
public interface IMSender {

    /**
     * 发送私聊消息
     */
    <T> void sendPrivateMessage(IMPrivateMessage<T> message);

    /**
     * 发送群聊消息
     */
    <T> void sendGroupMessage(IMGroupMessage<T> message);
}
