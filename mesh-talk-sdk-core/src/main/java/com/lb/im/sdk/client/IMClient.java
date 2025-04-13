package com.lb.im.sdk.client;

import com.lb.im.common.domain.model.IMGroupMessage;
import com.lb.im.common.domain.model.IMPrivateMessage;

public interface IMClient {

    <T> void sendPrivateMessage(IMPrivateMessage<T> message);

    <T> void sendGroupMessage(IMGroupMessage<T> message);
}
