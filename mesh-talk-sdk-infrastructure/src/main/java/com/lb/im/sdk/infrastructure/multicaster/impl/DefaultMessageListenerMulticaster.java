package com.lb.im.sdk.infrastructure.multicaster.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.lb.im.common.domain.enums.IMListenerType;
import com.lb.im.common.domain.model.IMSendResult;
import com.lb.im.sdk.domain.annotation.IMListener;
import com.lb.im.sdk.domain.listener.MessageListener;
import com.lb.im.sdk.infrastructure.multicaster.MessageListenerMulticaster;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

/**
 * 消息监听器多播器的默认实现
 * 
 * 负责将消息发送结果广播给所有注册的监听器。
 * 使用Spring的依赖注入自动收集所有实现了MessageListener接口的Bean，
 * 并根据它们的IMListener注解类型进行过滤，将消息分发给匹配的监听器。
 */
@Component
public class DefaultMessageListenerMulticaster implements MessageListenerMulticaster {

    /**
     * 所有注册的消息监听器列表
     * 通过Spring自动注入所有实现了MessageListener接口的Bean
     * required=false表示如果没有监听器也不会报错
     */
    @Autowired(required = false)
    private List<MessageListener> messageListenerList = Collections.emptyList();

    /**
     * 将消息结果广播给所有匹配的监听器
     *
     * @param listenerType 监听器类型，用于筛选需要处理该消息的监听器
     * @param result       消息发送结果对象，包含需要处理的数据
     */
    @Override
    public <T> void multicast(IMListenerType listenerType, IMSendResult result) {
        // 如果没有注册的监听器，直接返回
        if (CollectionUtil.isEmpty(messageListenerList)) {
            return;
        }

        // 遍历所有监听器，筛选符合条件的监听器进行处理
        messageListenerList.forEach(messageListener -> {
            // 获取监听器上的IMListener注解
            IMListener imListener = messageListener.getClass().getAnnotation(IMListener.class);

            // 检查监听器是否应该接收此类型的消息
            // 如果监听器类型是ALL或者与指定的类型匹配，则处理消息
            if (imListener != null && (IMListenerType.ALL.equals(imListener.listenerType()) || imListener.listenerType().equals(listenerType))) {
                // 处理JSON数据类型转换
                if (result.getData() instanceof JSONObject) {
                    // 使用反射获取监听器接口的泛型类型
                    Type superInterface = messageListener.getClass().getGenericInterfaces()[0];
                    Type type = ((ParameterizedType) superInterface).getActualTypeArguments()[0];

                    // 将JSON数据转换为目标监听器接口的泛型类型
                    JSONObject data = (JSONObject) result.getData();
                    result.setData(data.toJavaObject(type));
                }

                // 调用监听器的处理方法
                messageListener.doProcess(result);
            }
        });
    }
}
