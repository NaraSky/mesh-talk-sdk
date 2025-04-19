package com.lb.im.sdk.client.impl;

import com.lb.im.common.domain.enums.IMTerminalType;
import com.lb.im.common.domain.model.IMGroupMessage;
import com.lb.im.common.domain.model.IMPrivateMessage;
import com.lb.im.sdk.client.IMClient;
import com.lb.im.sdk.interfaces.sender.IMSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 即时通讯客户端接口的默认实现
 * 
 * 该类实现了IMClient接口，作为应用程序与即时通讯系统交互的桥梁。
 * 采用代理模式，将所有方法调用委托给IMSender接口的实现类，
 * 实现了客户端接口与具体消息发送实现的解耦，便于维护和扩展。
 */
@Service
public class DefaultIMClient implements IMClient {

    /**
     * 消息发送器，负责实际的消息发送和状态查询逻辑
     */
    @Autowired
    private IMSender imSender;

    /**
     * 发送私聊消息
     * 将请求委托给IMSender处理
     * 
     * @param message 私聊消息对象
     */
    @Override
    public <T> void sendPrivateMessage(IMPrivateMessage<T> message) {
        imSender.sendPrivateMessage(message);
    }

    /**
     * 发送群聊消息
     * 将请求委托给IMSender处理
     * 
     * @param message 群聊消息对象
     */
    @Override
    public <T> void sendGroupMessage(IMGroupMessage<T> message) {
        imSender.sendGroupMessage(message);
    }

    /**
     * 检查用户是否在线
     * 将请求委托给IMSender处理
     * 
     * @param userId 用户ID
     * @return 用户在线状态
     */
    @Override
    public Boolean isOnline(Long userId) {
        return imSender.isOnline(userId);
    }

    /**
     * 获取在线用户列表
     * 将请求委托给IMSender处理
     * 
     * @param userIds 用户ID列表
     * @return 在线用户ID列表
     */
    @Override
    public List<Long> getOnlineUserList(List<Long> userIds) {
        return imSender.getOnlineUser(userIds);
    }

    /**
     * 获取用户在线终端信息
     * 将请求委托给IMSender处理
     * 
     * @param userIds 用户ID列表
     * @return 用户ID到在线终端类型的映射
     */
    @Override
    public Map<Long, List<IMTerminalType>> getOnlineTerminal(List<Long> userIds) {
        return imSender.getOnlineTerminal(userIds);
    }
}
