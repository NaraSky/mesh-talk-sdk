package com.lb.im.sdk.application.consumer;

import cn.hutool.core.util.StrUtil;
import com.lb.im.common.domain.constans.IMConstants;
import com.lb.im.common.domain.enums.IMListenerType;
import com.lb.im.common.domain.model.IMSendResult;
import com.lb.im.sdk.infrastructure.multicaster.MessageListenerMulticaster;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 私聊消息结果消费者
 * 负责从RocketMQ消息队列中消费私聊消息的发送结果，并将结果广播给相应的监听器
 */
@Component
@ConditionalOnProperty(name = "message.mq.type", havingValue = "rocketmq") // 仅在使用RocketMQ作为消息队列时启用
@RocketMQMessageListener(
        consumerGroup = IMConstants.IM_RESULT_PRIVATE_CONSUMER_GROUP, // 消费者组名称
        topic = IMConstants.IM_RESULT_PRIVATE_QUEUE                   // 订阅的主题
)
public class PrivateMessageResultConsumer extends BaseMessageResultConsumer implements RocketMQListener<String> {

    // 日志记录器
    private final Logger logger = LoggerFactory.getLogger(PrivateMessageResultConsumer.class);

    // 消息监听器多播器，用于将消息广播给所有注册的监听器
    @Autowired
    private MessageListenerMulticaster messageListenerMulticaster;

    /**
     * 处理从消息队列接收到的消息
     * 
     * @param message 从RocketMQ接收到的原始消息字符串
     */
    @Override
    public void onMessage(String message) {
        // 检查消息是否为空
        if (StrUtil.isEmpty(message)) {
            logger.warn("PrivateMessageResultConsumer.onMessage|接收到的消息为空");
            return;
        }

        // 解析消息内容为IMSendResult对象
        IMSendResult<?> imSendResult = this.getResultMessage(message);
        if (imSendResult == null) {
            logger.warn("PrivateMessageResultConsumer.onMessage|转化后的数据为空");
            return;
        }

        // 将消息结果广播给所有注册的私聊消息监听器
        messageListenerMulticaster.multicast(IMListenerType.PRIVATE_MESSAGE, imSendResult);
    }
}
