package com.lb.im.sdk.infrastructure.multicaster;

import com.lb.im.common.domain.enums.IMListenerType;
import com.lb.im.common.domain.model.IMSendResult;

/**
 * 广播消息
 */
public interface MessageListenerMulticaster {

    /**
     * 广播消息
     *
     * @param listenerType 监听的类型
     * @param result       发送消息的结果
     * @param <T>          泛型类型
     */
    <T> void multicast(IMListenerType listenerType, IMSendResult result);

}
