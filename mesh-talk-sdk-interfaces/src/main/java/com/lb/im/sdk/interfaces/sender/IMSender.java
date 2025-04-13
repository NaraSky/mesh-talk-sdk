package com.lb.im.sdk.interfaces.sender;

import com.lb.im.common.domain.enums.IMDeviceType;
import com.lb.im.common.domain.model.IMGroupMessage;
import com.lb.im.common.domain.model.IMPrivateMessage;
import com.lb.im.common.domain.model.IMSendResult;

import java.util.List;
import java.util.Map;

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

    /**
     * 获取在线终端数据
     * key-用户id，value-当前用户的终端列表
     */
    Map<Long, List<IMDeviceType>> getOnlineTerminal(List<Long> userIds);

    /**
     * 判断用户是否在线
     */
    Boolean isOnline(Long userId);

    /**
     * 筛选在线的用户
     */
    List<Long> getOnlineUser(List<Long> userIds);
}
