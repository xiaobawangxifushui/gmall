package com.atguigu.gmall.wms.listenner;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.wms.dao.WareSkuDao;
import com.atguigu.gmall.wms.vo.SkuWareVo;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class WareListenner {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private WareSkuDao wareSkuDao;
    private static final String WARE_PREFIX = "ware:unlock:";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "unlock", durable = "true"),
            exchange = @Exchange(value = "GMALL-WARE-EXCHANGE", type = ExchangeTypes.TOPIC, ignoreDeclarationExceptions = "true"),
            key = {"ware.unlock"}
    ))
    public void listen(String orderToken) {
        String s = redisTemplate.opsForValue().get(WARE_PREFIX + orderToken);
        if (StringUtils.isEmpty(s)){
            return;
        }
        List<SkuWareVo> skuWareVoList = JSON.parseArray(s, SkuWareVo.class);
        if (CollectionUtils.isEmpty(skuWareVoList)){
            return;
        }
        skuWareVoList.forEach(skuWareVo -> {
            wareSkuDao.rollBack(skuWareVo.getWareId(),skuWareVo.getCount());
        });
    }
}
