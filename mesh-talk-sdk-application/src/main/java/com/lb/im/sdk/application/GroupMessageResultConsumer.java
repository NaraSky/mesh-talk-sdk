package com.lb.im.sdk.application;

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

@Component
@ConditionalOnProperty(name = "message.mq.type", havingValue = "rocketmq")
@RocketMQMessageListener(consumerGroup = IMConstants.IM_RESULT_GROUP_CONSUMER_GROUP, topic = IMConstants.IM_RESULT_GROUP_QUEUE)
public class GroupMessageResultConsumer extends BaseMessageResultConsumer implements RocketMQListener<String> {

    private final Logger logger = LoggerFactory.getLogger(GroupMessageResultConsumer.class);

    @Autowired
    private MessageListenerMulticaster messageListenerMulticaster;

    @Override
    public void onMessage(String message) {
        if (StrUtil.isEmpty(message)) {
            logger.warn("GroupMessageResultConsumer.onMessage|接收到的消息为空");
            return;
        }
        IMSendResult<?> imSendResult = this.getResultMessage(message);
        if (imSendResult == null) {
            logger.warn("GroupMessageResultConsumer.onMessage|转化后的数据为空");
            return;
        }
        messageListenerMulticaster.multicast(IMListenerType.GROUP_MESSAGE, imSendResult);
    }
}
