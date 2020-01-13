package com.atguigu.gmall.search.listenner;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.GoodsRepostory;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValue;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PmsListenner {
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GoodsRepostory goodsRepostory;
    @Autowired
    private GmallWmsClient wmsClient;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "GMALL-SEARCH-QUEUE",durable = "true")
            ,exchange = @Exchange(value = "GMALL-PMS-EXCHANGE",type = ExchangeTypes.TOPIC,ignoreDeclarationExceptions = "true")
            ,key = {"iterm.insert"}))
    public void listenner(Long spuId){
        System.out.println(111111);
        Resp<List<SkuInfoEntity>> skuResp = pmsClient.querySkuInfosBySpuId(spuId);
        List<SkuInfoEntity> skuInfoEntities = skuResp.getData();
        if (!CollectionUtils.isEmpty(skuInfoEntities)) {
            List<Goods> goodsList = skuInfoEntities.stream().map(skuInfoEntity -> {
                Goods goods = new Goods();
                goods.setSkuId(skuInfoEntity.getSkuId());
                goods.setSkuTitle(skuInfoEntity.getSkuTitle());
                goods.setSkuSubTitle(skuInfoEntity.getSkuSubtitle());
                goods.setPrice(skuInfoEntity.getPrice().doubleValue());
                goods.setDefaultImage(skuInfoEntity.getSkuDefaultImg());

                goods.setSale(10000l);
                Resp<List<WareSkuEntity>> wareResp = wmsClient.qureyWareSkuBySkuId(skuInfoEntity.getSkuId());
                List<WareSkuEntity> wareSkuEntities = wareResp.getData();
                if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                    goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
                }
                Resp<SpuInfoEntity> spuInfoEntityResp = pmsClient.info(spuId);
                SpuInfoEntity spuInfoEntity = spuInfoEntityResp.getData();
                goods.setCreateTime(spuInfoEntity.getCreateTime());

                Resp<BrandEntity> brandEntityResp = pmsClient.queryBrand(skuInfoEntity.getBrandId());
                BrandEntity brandEntity = brandEntityResp.getData();
                if (brandEntity != null) {
                    goods.setBrandId(skuInfoEntity.getBrandId());
                    goods.setBrandName(brandEntity.getName());
                }

                Resp<CategoryEntity> categoryEntityResp = pmsClient.queryCate(skuInfoEntity.getCatalogId());
                CategoryEntity categoryEntity = categoryEntityResp.getData();
                if (categoryEntity != null) {
                    goods.setCategoryId(skuInfoEntity.getCatalogId());
                    goods.setCategoryName(categoryEntity.getName());
                }

                Resp<List<ProductAttrValueEntity>> listResp = pmsClient.queryProBySkuId(spuId);
                List<ProductAttrValueEntity> productAttrValueEntities = listResp.getData();
                if (!CollectionUtils.isEmpty(productAttrValueEntities)) {
                    List<SearchAttrValue> searchAttrValues = productAttrValueEntities.stream().map(productAttrValueEntity -> {
                        SearchAttrValue searchAttrValue = new SearchAttrValue();
                        searchAttrValue.setAttrId(productAttrValueEntity.getAttrId());
                        searchAttrValue.setAttrName(productAttrValueEntity.getAttrName());
                        searchAttrValue.setAttrValue(productAttrValueEntity.getAttrValue());
                        return searchAttrValue;
                    }).collect(Collectors.toList());
                    goods.setAttrs(searchAttrValues);
                }
                return goods;
            }).collect(Collectors.toList());
            goodsRepostory.saveAll(goodsList);
        }

    }
}
