package com.lb.im.sdk.domain.listener;

import com.lb.im.common.domain.model.IMSendResult;

public interface MessageListener<T> {

    /**
     * 处理发送的结果
     */
    void doProcess(IMSendResult<T> result);
}
