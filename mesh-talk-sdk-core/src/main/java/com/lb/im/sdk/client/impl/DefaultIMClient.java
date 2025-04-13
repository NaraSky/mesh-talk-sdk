package com.lb.im.sdk.client.impl;

import com.lb.im.common.domain.model.IMGroupMessage;
import com.lb.im.common.domain.model.IMPrivateMessage;
import com.lb.im.sdk.client.IMClient;
import com.lb.im.sdk.interfaces.sender.IMSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DefaultIMClient implements IMClient {

    @Autowired
    private IMSender imSender;

    @Override
    public <T> void sendPrivateMessage(IMPrivateMessage<T> message) {
        imSender.sendPrivateMessage(message);
    }

    @Override
    public <T> void sendGroupMessage(IMGroupMessage<T> message) {
        imSender.sendGroupMessage(message);
    }
}
