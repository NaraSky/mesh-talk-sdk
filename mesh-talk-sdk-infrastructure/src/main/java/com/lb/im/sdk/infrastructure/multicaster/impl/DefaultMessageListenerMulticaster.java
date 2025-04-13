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

@Component
public class DefaultMessageListenerMulticaster implements MessageListenerMulticaster {

    @Autowired(required = false)
    private List<MessageListener> messageListenerList = Collections.emptyList();

    /**
     * 根据指定的监听器类型，将消息事件分发给所有符合条件的监听器进行处理。
     *
     * @param listenerType 监听器类型，用于筛选需要处理该消息的监听器
     * @param result       消息发送结果对象，包含需要处理的数据
     */
    @Override
    public <T> void multicast(IMListenerType listenerType, IMSendResult result) {
        if (CollectionUtil.isEmpty(messageListenerList)) {
            return;
        }
        // 遍历所有监听器，筛选符合条件的监听器进行处理
        messageListenerList.forEach(messageListener -> {
            IMListener imListener = messageListener.getClass().getAnnotation(IMListener.class);
            if (imListener != null && (IMListenerType.ALL.equals(imListener.listenerType()) || imListener.listenerType().equals(listenerType))) {
                if (result.getData() instanceof JSONObject) {
                    // 将JSON数据转换为目标监听器接口的泛型类型
                    Type superInterface = messageListener.getClass().getGenericInterfaces()[0];
                    Type type = ((ParameterizedType) superInterface).getActualTypeArguments()[0];
                    JSONObject data = (JSONObject) result.getData();
                    result.setData(data.toJavaObject(type));
                }
                messageListener.doProcess(result);
            }
        });
    }
}
