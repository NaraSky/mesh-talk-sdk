package com.lb.im.sdk.client.impl;

import com.lb.im.common.domain.enums.IMDeviceType;
import com.lb.im.common.domain.model.IMGroupMessage;
import com.lb.im.common.domain.model.IMPrivateMessage;
import com.lb.im.sdk.client.IMClient;
import com.lb.im.sdk.interfaces.sender.IMSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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

    @Override
    public Boolean isOnline(Long userId) {
        return imSender.isOnline(userId);
    }

    @Override
    public List<Long> getOnlineUserList(List<Long> userIds) {
        return imSender.getOnlineUser(userIds);
    }

    @Override
    public Map<Long, List<IMDeviceType>> getOnlineTerminal(List<Long> userIds) {
        return imSender.getOnlineTerminal(userIds);
    }
}
