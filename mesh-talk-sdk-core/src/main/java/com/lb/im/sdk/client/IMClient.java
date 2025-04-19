package com.lb.im.sdk.client;

import com.lb.im.common.domain.enums.IMTerminalType;
import com.lb.im.common.domain.model.IMGroupMessage;
import com.lb.im.common.domain.model.IMPrivateMessage;

import java.util.List;
import java.util.Map;

/**
 * 即时通讯客户端接口
 * 
 * 该接口定义了即时通讯系统的核心功能，是应用程序与即时通讯系统交互的主要入口。
 * 提供消息发送和用户在线状态查询等基本功能。
 */
public interface IMClient {

    /**
     * 发送私聊消息
     * 
     * @param message 私聊消息对象，包含发送者、接收者和消息内容等信息
     * @param <T> 消息内容的类型参数
     */
    <T> void sendPrivateMessage(IMPrivateMessage<T> message);

    /**
     * 发送群聊消息
     * 
     * @param message 群聊消息对象，包含发送者、群组ID和消息内容等信息
     * @param <T> 消息内容的类型参数
     */
    <T> void sendGroupMessage(IMGroupMessage<T> message);

    /**
     * 检查指定用户是否在线
     * 
     * @param userId 需要检查的用户ID
     * @return 如果用户至少在一个终端上在线则返回true，否则返回false
     */
    Boolean isOnline(Long userId);

    /**
     * 从给定的用户ID列表中筛选出当前在线的用户
     * 
     * @param userIds 需要检查的用户ID列表
     * @return 当前在线的用户ID列表
     */
    List<Long> getOnlineUserList(List<Long> userIds);

    /**
     * 获取指定用户在各个终端的在线状态
     * 
     * @param userIds 需要查询的用户ID列表
     * @return 用户ID到在线终端类型列表的映射，键为用户ID，值为该用户当前在线的终端类型列表
     */
    Map<Long, List<IMTerminalType>> getOnlineTerminal(List<Long> userIds);
}
