package com.lb.im.sdk.application;

import com.alibaba.fastjson.JSONObject;
import com.lb.im.common.domain.constans.IMConstants;
import com.lb.im.common.domain.model.IMSendResult;

public class BaseMessageResultConsumer {

    /**
     * 解析数据
     */
    protected IMSendResult<?> getResultMessage(String msg){
        JSONObject jsonObject = JSONObject.parseObject(msg);
        String eventStr = jsonObject.getString(IMConstants.MSG_KEY);
        return JSONObject.parseObject(eventStr, IMSendResult.class);
    }
}