package com.lb.im.sdk.client;

import com.lb.im.common.domain.enums.IMDeviceType;
import com.lb.im.common.domain.model.IMGroupMessage;
import com.lb.im.common.domain.model.IMPrivateMessage;

import java.util.List;
import java.util.Map;

public interface IMClient {

    <T> void sendPrivateMessage(IMPrivateMessage<T> message);

    <T> void sendGroupMessage(IMGroupMessage<T> message);

    Boolean isOnline(Long userId);

    List<Long> getOnlineUserList(List<Long> userIds);

    Map<Long,List<IMDeviceType>> getOnlineTerminal(List<Long> userIds);
}
