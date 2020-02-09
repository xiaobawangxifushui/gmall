package com.atguigu.gmall.cart.listenner;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.cart.feign.GmallPmsClieny;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

@Component
public class PriceListenner {
    @Autowired
    private GmallPmsClieny pmsClieny;
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "cart:item:";

    private static final String PRICE_PREFIX = "cart:price:";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "price", durable = "true"),
            exchange = @Exchange(value = "GMALL-PMS-EXCHANGE", type = ExchangeTypes.TOPIC, ignoreDeclarationExceptions = "true"),
            key = {"item.update"}
    ))
    public void listen(Long spuId) {
        Resp<List<SkuInfoEntity>> listResp = pmsClieny.querySkuInfosBySpuId(spuId);
        List<SkuInfoEntity> skuInfoEntities = listResp.getData();
        if (!CollectionUtils.isEmpty(skuInfoEntities)) {
            skuInfoEntities.forEach(skuInfoEntity -> redisTemplate.opsForValue().set(PRICE_PREFIX + skuInfoEntity.getSkuId(), skuInfoEntity.getPrice().toString()));
        }
    }
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "delete", durable = "true"),
            exchange = @Exchange(value = "GMALL-ORDER-EXCHANGE", type = ExchangeTypes.TOPIC, ignoreDeclarationExceptions = "true"),
            key = {"cart.delete"}
    ))
    public void delete(Map map) {
        if (CollectionUtils.isEmpty(map)){
            return;
        }
        Long userId = (Long) map.get("userId");
        String skuIds = map.get("skuIds").toString();
        List<String> ids = JSON.parseArray(skuIds, String.class);
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);
        hashOps.delete(ids.toArray());
    }

}
