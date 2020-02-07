package com.atguigu.gmall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.cart.Pojo.Cart;
import com.atguigu.gmall.cart.Pojo.UserInfo;
import com.atguigu.gmall.cart.feign.GmallPmsClieny;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.CartInterceptor;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService{

    private static final String KEY_PREFIX = "cart:item:";

    private static final String PRICE_PREFIX = "cart:price:";

    @Autowired
    private StringRedisTemplate template;
    @Autowired
    private GmallPmsClieny pmsClieny;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GmallSmsClient smsClient;

    @Override
    public void insert(Cart cart) {
        Long skuId = cart.getSkuId();
        Integer count = cart.getCount();
        String key = KEY_PREFIX;
        UserInfo userInfo = CartInterceptor.getUserInfo();
        if (userInfo.getUserId()!=null){
            key += userInfo.getUserId();
        }else {
            key += userInfo.getUserKey();
        }

        BoundHashOperations<String, Object, Object> hashOps = template.boundHashOps(key);

        String skuIdString = skuId.toString();

        if (hashOps.hasKey(skuIdString)){
            String cartJson = hashOps.get(skuIdString).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(cart.getCount()+count);
        }else{
            cart.setCheck(true);
            cart.setSkuId(skuId);
            cart.setCount(count);

            Resp<SkuInfoEntity> skuInfoEntityResp = pmsClieny.querySkuById(skuId);
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
            if (skuInfoEntity==null){
                return;
            }
            cart.setImage(skuInfoEntity.getSkuDefaultImg());
            cart.setPrice(skuInfoEntity.getPrice());
            cart.setSkuTitle(skuInfoEntity.getSkuTitle());

            Resp<List<WareSkuEntity>> wareSkuResp = wmsClient.qureyWareSkuBySkuId(skuId);
            List<WareSkuEntity> wareSkuEntities = wareSkuResp.getData();
            cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock()>0));

            Resp<List<SkuSaleAttrValueEntity>> saleAttrResp = pmsClieny.querySaleAttrBySkuId(skuId);
            List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = saleAttrResp.getData();
            cart.setSaleAttrs(skuSaleAttrValueEntities);

            Resp<List<ItemSaleVo>> saleResp = smsClient.querySaleVoBySkuId(skuId);
            List<ItemSaleVo> itemSaleVos = saleResp.getData();
            cart.setSales(itemSaleVos);
            template.opsForValue().set(PRICE_PREFIX+skuId,skuInfoEntity.getPrice().toString());
        }
        hashOps.put(skuIdString,JSON.toJSONString(cart));
    }

    @Override
    public List<Cart> cart() {
        UserInfo userInfo = CartInterceptor.getUserInfo();
        String userKey = KEY_PREFIX+userInfo.getUserKey();
        Long userId = userInfo.getUserId();

        BoundHashOperations<String, Object, Object> userkeyHashOps = template.boundHashOps(userKey);
        List<Object> values = userkeyHashOps.values();
        List<Cart> userKeyCarts = null;
        if (!CollectionUtils.isEmpty(values)){
            userKeyCarts = values.stream().map(cartJson->{
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                String currentPrice = template.opsForValue().get(PRICE_PREFIX + cart.getSkuId());
                cart.setCurntPrice(new BigDecimal(currentPrice));
                return cart;
            }).collect(Collectors.toList());
        }
        if (userId==null){
            return userKeyCarts;
        }

        BoundHashOperations<String, Object, Object> userIdHashOps = template.boundHashOps(KEY_PREFIX + userId);
        if (!CollectionUtils.isEmpty(userKeyCarts)){
            userKeyCarts.forEach(cart -> {
                if (userIdHashOps.hasKey(cart.getSkuId().toString())){
                    String cartString = userIdHashOps.get(cart.getSkuId().toString()).toString();
                    Integer count = cart.getCount();
                    cart = JSON.parseObject(cartString, Cart.class);
                    cart.setCount(cart.getCount()+count);
                    String currentPrice = template.opsForValue().get(PRICE_PREFIX + cart.getSkuId());
                    cart.setCurntPrice(new BigDecimal(currentPrice));
                }
                userIdHashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
            });
            template.delete(userKey);

        }

        List<Object> userIdCartJsons = userIdHashOps.values();
        if(!CollectionUtils.isEmpty(userIdCartJsons)){
            return userIdCartJsons.stream().map(cartJson->{
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                String currentPrice = template.opsForValue().get(PRICE_PREFIX + cart.getSkuId());
                if (StringUtils.isNotBlank(currentPrice)) {
                    cart.setCurntPrice(new BigDecimal(currentPrice));
                }
                return cart;
            }).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public void updateCount(Cart cart) {
        UserInfo userInfo = CartInterceptor.getUserInfo();
        String key = KEY_PREFIX;
        if (userInfo.getUserId()!=null){
            key += userInfo.getUserId();
        }else{
            key += userInfo.getUserKey();
        }

        BoundHashOperations<String, Object, Object> hashOps = template.boundHashOps(key);
        if (hashOps.hasKey(cart.getSkuId().toString())){
            String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
            Integer count = cart.getCount();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(count);
            hashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
        }
    }

    @Override
    public void check(Cart cart) {
        UserInfo userInfo = CartInterceptor.getUserInfo();
        String key = KEY_PREFIX;
        if (userInfo.getUserId()!=null){
            key += userInfo.getUserId();
        }else{
            key += userInfo.getUserKey();
        }

        BoundHashOperations<String, Object, Object> hashOps = template.boundHashOps(key);
        if (hashOps.hasKey(cart.getSkuId().toString())){
            String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
            Boolean check = cart.getCheck();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCheck(check);
            hashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
        }
    }

    @Override
    public void delete(Long skuId) {
        UserInfo userInfo = CartInterceptor.getUserInfo();
        String key = KEY_PREFIX;
        if (userInfo.getUserId()!=null){
            key += userInfo.getUserId();
        }else{
            key += userInfo.getUserKey();
        }

        BoundHashOperations<String, Object, Object> hashOps = template.boundHashOps(key);
        if (hashOps.hasKey(skuId.toString())){
            hashOps.delete(skuId.toString());
        }
    }
}
