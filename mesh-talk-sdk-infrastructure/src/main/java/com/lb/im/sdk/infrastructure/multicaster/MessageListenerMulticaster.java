package com.lb.im.sdk.infrastructure.multicaster;

import com.lb.im.common.domain.enums.IMListenerType;
import com.lb.im.common.domain.model.IMSendResult;

/**
 * 消息监听器多播器接口
 * 
 * 负责将消息发送结果广播给所有注册的监听器。
 * 在即时通讯系统中，当消息发送完成后，需要通知相关的监听器处理结果，
 * 例如更新UI、记录日志、触发业务逻辑等。多播器提供了一种解耦的方式，
 * 使得消息发送者不需要直接依赖具体的监听器实现。
 */
public interface MessageListenerMulticaster {

    /**
     * 将消息结果广播给所有匹配的监听器
     *
     * @param listenerType 监听器类型，用于筛选需要接收此消息的监听器
     *                    （可以是PRIVATE_MESSAGE、GROUP_MESSAGE或ALL）
     * @param result       消息发送结果对象，包含发送状态和消息内容
     * @param <T>          消息内容的泛型类型
     */
    <T> void multicast(IMListenerType listenerType, IMSendResult result);

}
