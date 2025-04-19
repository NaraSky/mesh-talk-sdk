package com.lb.im.sdk.application.consumer;

import com.alibaba.fastjson.JSONObject;
import com.lb.im.common.domain.constans.IMConstants;
import com.lb.im.common.domain.model.IMSendResult;

/**
 * 消息结果消费者的基类
 * 提供消息解析的公共功能，被私聊和群聊消息结果消费者继承
 */
public class BaseMessageResultConsumer {

    /**
     * 解析消息数据
     * 
     * @param msg 从消息队列接收到的原始JSON字符串
     * @return 解析后的消息发送结果对象，包含发送状态和内容
     */
    protected IMSendResult<?> getResultMessage(String msg){
        // 将接收到的字符串解析为JSON对象
        JSONObject jsonObject = JSONObject.parseObject(msg);
        // 从JSON对象中提取消息内容字段
        String eventStr = jsonObject.getString(IMConstants.MSG_KEY);
        // 将消息内容转换为IMSendResult对象并返回
        return JSONObject.parseObject(eventStr, IMSendResult.class);
    }
}
