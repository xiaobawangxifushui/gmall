package com.atguigu.gmall.order.service.Impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.core.exception.OrderException;
import com.atguigu.gmall.cart.Pojo.Cart;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptor.CartInterceptor;
import com.atguigu.gmall.order.service.OrderService;


import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuWareVo;
import com.atguigu.gmall.order.vo.OrderConfirmVo;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private GmallCartClient cartClient;

    @Autowired
    private GmallPmsClieny pmsClieny;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallOmsClient omsClient;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ThreadPoolExecutor threadPool;

    private static final String ORDER_TOKEN = "order:token:";

    @Override
    public OrderConfirmVo comfirm() {

        UserInfo userInfo = CartInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();


        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();

        CompletableFuture<Void> addressFuture = CompletableFuture.runAsync(() -> {
            Resp<List<MemberReceiveAddressEntity>> addressResp = umsClient.queryAddressByUserId(userId);
            List<MemberReceiveAddressEntity> memberReceiveAddressEntities = addressResp.getData();
            orderConfirmVo.setAddresses(memberReceiveAddressEntities);
        }, threadPool);

        CompletableFuture<Void> itemFuture = CompletableFuture.supplyAsync(() -> {
            Resp<List<Cart>> cartsResp = cartClient.queryCheckedCartByUserId(userId);
            return cartsResp;
        }, threadPool).thenAcceptAsync(cartsResp -> {
            List<Cart> cartList = cartsResp.getData();
            if (!CollectionUtils.isEmpty(cartList)) {
                List<OrderItemVo> orderItemVoList = cartList.stream().map(cart -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    Long skuId = cart.getSkuId();
                    orderItemVo.setSkuId(skuId);
                    orderItemVo.setCount(cart.getCount());

                    CompletableFuture<Void> skuFuture = CompletableFuture.runAsync(() -> {
                        Resp<SkuInfoEntity> skuInfoEntityResp = pmsClieny.querySkuById(skuId);
                        SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
                        if (skuInfoEntity != null) {
                            orderItemVo.setImage(skuInfoEntity.getSkuDefaultImg());
                            orderItemVo.setPrice(skuInfoEntity.getPrice());
                            orderItemVo.setSkuTitle(skuInfoEntity.getSkuTitle());
                            orderItemVo.setWeight(skuInfoEntity.getWeight());
                        }
                    }, threadPool);


                    CompletableFuture<Void> wareFuture = CompletableFuture.runAsync(() -> {
                        Resp<List<WareSkuEntity>> wareResp = wmsClient.qureyWareSkuBySkuId(skuId);
                        List<WareSkuEntity> wareSkuEntities = wareResp.getData();
                        if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                            orderItemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
                        }
                    }, threadPool);

                    CompletableFuture<Void> saleAttrFuture = CompletableFuture.runAsync(() -> {
                        Resp<List<SkuSaleAttrValueEntity>> saleAttrResp = pmsClieny.querySaleAttrBySkuId(skuId);
                        List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = saleAttrResp.getData();
                        orderItemVo.setSaleAttrs(skuSaleAttrValueEntities);
                    });

                    CompletableFuture<Void> salesFuture = CompletableFuture.runAsync(() -> {
                        Resp<List<ItemSaleVo>> salesResp = smsClient.querySaleVoBySkuId(skuId);
                        List<ItemSaleVo> itemSaleVos = salesResp.getData();
                        orderItemVo.setSales(itemSaleVos);
                    });

                    CompletableFuture.allOf(skuFuture, wareFuture, saleAttrFuture, salesFuture).join();

                    return orderItemVo;
                }).collect(Collectors.toList());
                orderConfirmVo.setOrderItemVos(orderItemVoList);
            }
        }, threadPool);


        CompletableFuture<Void> boundsFuture = CompletableFuture.runAsync(() -> {
            Resp<MemberEntity> boundsResp = umsClient.queryMemberById(userId);
            MemberEntity memberEntity = boundsResp.getData();
            if (memberEntity != null) {
                orderConfirmVo.setBounds(memberEntity.getIntegration());
            }
        }, threadPool);

        CompletableFuture<Void> orderTokenFuture = CompletableFuture.runAsync(() -> {
            String orderToken = IdWorker.getTimeId();
            orderConfirmVo.setOrderToken(orderToken);
            redisTemplate.opsForValue().set(ORDER_TOKEN + orderToken, orderToken, 3, TimeUnit.HOURS);
        }, threadPool);

        CompletableFuture.allOf(addressFuture, itemFuture, boundsFuture, orderTokenFuture).join();


        return orderConfirmVo;
    }

    @Override
    public void submit(OrderSubmitVo orderSubmitVo) {

        String orderToken = orderSubmitVo.getOrderToken();
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long flag = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(ORDER_TOKEN + orderToken), orderToken);
        if (flag == 0) {
            throw new OrderException("已将提交，请勿重复提交！");
        }


        BigDecimal totalPrice = orderSubmitVo.getTotalPrice();
        List<OrderItemVo> items = orderSubmitVo.getItems();
        orderSubmitVo.getItems();
        if (CollectionUtils.isEmpty(items)) {
            throw new OrderException("请选择商品！");
        }
        BigDecimal curruntTotalPrice = items.stream().map(item -> {
            Resp<SkuInfoEntity> skuInfoEntityResp = pmsClieny.querySkuById(item.getSkuId());
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
            if (skuInfoEntity != null) {
                return skuInfoEntity.getPrice().multiply(new BigDecimal(item.getCount()));
            }
            return new BigDecimal(0);
        }).reduce((a, b) -> a.add(b)).get();
        if (totalPrice.compareTo(curruntTotalPrice) != 0) {
            throw new OrderException("页面失效，重新刷新页面！");
        }

        List<SkuWareVo> skuWareVos = items.stream().map(orderItemVo -> {
            SkuWareVo skuWareVo = new SkuWareVo();
            skuWareVo.setSkuId(orderItemVo.getSkuId());
            skuWareVo.setCount(orderItemVo.getCount());
            skuWareVo.setOrderToken(orderToken);
            return skuWareVo;
        }).collect(Collectors.toList());

        Resp<List<SkuWareVo>> wareResp = wmsClient.checkAndLock(skuWareVos);
        List<SkuWareVo> skuWareVoList = wareResp.getData();
        if (!CollectionUtils.isEmpty(skuWareVoList)){
            throw new OrderException(JSON.toJSONString(skuWareVoList));
        }

        UserInfo userInfo = CartInterceptor.getUserInfo();
        try {
            omsClient.saveOrder(orderSubmitVo,userInfo.getUserId());
        } catch (Exception e) {
            e.printStackTrace();
            amqpTemplate.convertAndSend("GMALL-WARE-EXCHANGE","ware.unlock",orderSubmitVo.getOrderToken());
        }

        Map<String, Object> map = new HashMap<>();
        map.put("userId",userInfo.getUserId());
        List<Long> skuIds = items.stream().map(orderItemVo -> orderItemVo.getSkuId()).collect(Collectors.toList());
        map.put("skuIds",JSON.toJSONString(skuIds));
        amqpTemplate.convertAndSend("GMALL-ORDER-EXCHANGE","cart.delete",map);

    }
}
