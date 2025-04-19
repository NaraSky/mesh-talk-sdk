package com.lb.im.sdk.interfaces.sender;

import com.lb.im.common.domain.enums.IMTerminalType;
import com.lb.im.common.domain.model.IMGroupMessage;
import com.lb.im.common.domain.model.IMPrivateMessage;

import java.util.List;
import java.util.Map;

/**
 * 消息发送接口
 * 
 * 该接口定义了即时通讯系统中消息发送和用户在线状态查询的核心功能。
 * 作为系统的核心组件，它负责：
 * 1. 处理私聊和群聊消息的发送
 * 2. 查询用户在线状态
 * 3. 获取用户在线终端信息
 * 
 * 实现此接口的类需要处理消息路由、在线状态管理等底层逻辑。
 */
public interface IMSender {

    /**
     * 发送私聊消息
     * 
     * 将消息发送给指定的接收用户，并可选择性地发送给自己的其他终端。
     * 实现需要处理：
     * - 检查接收用户是否在线
     * - 将消息路由到正确的服务器
     * - 处理离线用户的消息状态回执
     * 
     * @param message 私聊消息对象，包含发送者、接收者和消息内容等信息
     * @param <T> 消息内容的类型参数
     */
    <T> void sendPrivateMessage(IMPrivateMessage<T> message);

    /**
     * 发送群聊消息
     * 
     * 将消息发送给群组中的所有成员，并可选择性地发送给自己的其他终端。
     * 实现需要处理：
     * - 将接收用户按服务器分组
     * - 批量发送消息
     * - 处理离线用户的消息状态回执
     * 
     * @param message 群聊消息对象，包含发送者、群组ID和消息内容等信息
     * @param <T> 消息内容的类型参数
     */
    <T> void sendGroupMessage(IMGroupMessage<T> message);

    /**
     * 获取在线终端数据
     * 
     * 查询指定用户列表中每个用户当前在线的终端类型。
     * 
     * @param userIds 需要查询的用户ID列表
     * @return 用户ID到终端类型列表的映射，键为用户ID，值为该用户当前在线的终端类型列表
     */
    Map<Long, List<IMTerminalType>> getOnlineTerminal(List<Long> userIds);

    /**
     * 判断用户是否在线
     * 
     * 检查指定用户是否至少在一个终端上在线。
     * 
     * @param userId 需要检查的用户ID
     * @return 如果用户至少在一个终端上在线则返回true，否则返回false
     */
    Boolean isOnline(Long userId);

    /**
     * 筛选在线的用户
     * 
     * 从给定的用户ID列表中筛选出当前在线的用户。
     * 
     * @param userIds 需要检查的用户ID列表
     * @return 当前在线的用户ID列表
     */
    List<Long> getOnlineUser(List<Long> userIds);
}
