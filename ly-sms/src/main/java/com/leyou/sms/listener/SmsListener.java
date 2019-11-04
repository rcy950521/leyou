package com.leyou.sms.listener;

import com.leyou.common.constants.MQConstants;
import com.leyou.sms.service.SendCheckCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class SmsListener {

    @Autowired
    private SendCheckCodeUtils checkCodeUtils;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MQConstants.Queue.SMS_VERIFY_CODE_QUEUE,durable = "true"),
            exchange = @Exchange(value = MQConstants.Exchange.SMS_EXCHANGE_NAME,type = ExchangeTypes.TOPIC),
            key = MQConstants.RoutingKey.VERIFY_CODE_KEY
    ))
    public void sendCheckCodeMsg(Map<String,String> codeMap){
        try {
            String code = codeMap.get("code");
            String phone = codeMap.get("phone");
            checkCodeUtils.sendCheckCodeMsg(phone,code);
        }catch (Exception e){
            log.error("MQ发送短信异常，异常信息为:{}",e);
        }
    }

}
