package com.atguigu.gmall.item.service.impl;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.item.feign.GmallPmsClieny;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.sun.xml.internal.ws.util.CompletedFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {
    @Autowired
    private GmallPmsClieny pmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private ThreadPoolExecutor threadPool;

    @Override
    public ItemVo queryItemVoBySkuId(Long skuId) {
        ItemVo itemVo = new ItemVo();

        itemVo.setSkuId(skuId);
        CompletableFuture<SkuInfoEntity> skuCompletableFuture = CompletableFuture.supplyAsync(() -> {
            Resp<SkuInfoEntity> skuInfoEntityResp = pmsClient.querySkuById(skuId);
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
            if (skuInfoEntity == null) {
                itemVo.setSkuId(null);
                return null;
            }
            itemVo.setSkuTitle(skuInfoEntity.getSkuTitle());
            itemVo.setSkuSubTitle(skuInfoEntity.getSkuSubtitle());
            itemVo.setPrice(skuInfoEntity.getPrice());
            itemVo.setWeight(skuInfoEntity.getWeight());
            return skuInfoEntity;
        },threadPool);


        CompletableFuture<Void> wareCompletableFuture = CompletableFuture.runAsync(() -> {
            Resp<List<WareSkuEntity>> wareSkuResp = wmsClient.qureyWareSkuBySkuId(skuId);
            List<WareSkuEntity> wareSkuEntities = wareSkuResp.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                itemVo.setStock(wareSkuEntities.stream().anyMatch(e -> e.getStock() > 0));
            } else {
                itemVo.setStock(false);
            }
        },threadPool);

        CompletableFuture<Void> imageCompletableFuture = CompletableFuture.runAsync(() -> {
            Resp<List<SkuImagesEntity>> imageBySkuIdResp = pmsClient.queryImageBySkuId(skuId);
            List<SkuImagesEntity> skuImagesEntities = imageBySkuIdResp.getData();
            itemVo.setImages(skuImagesEntities);
        },threadPool);


        CompletableFuture<Void> spuCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            if (skuInfoEntity == null) {
                return ;
            }
            itemVo.setSpuId(skuInfoEntity.getSpuId());
            Resp<SpuInfoEntity> spuInfoEntityResp = pmsClient.info(skuInfoEntity.getSpuId());
            SpuInfoEntity spuInfoEntity = spuInfoEntityResp.getData();
            if (spuInfoEntity != null) {
                itemVo.setSpuName(spuInfoEntity.getSpuName());
            }
        },threadPool);

        CompletableFuture<Void> descCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            if (skuInfoEntity == null) {
                return ;
            }
            Resp<SpuInfoDescEntity> desc = pmsClient.desc(skuInfoEntity.getSpuId());
            SpuInfoDescEntity spuInfoDescEntity = desc.getData();
            itemVo.setDesc(spuInfoDescEntity);
        },threadPool);

        CompletableFuture<Void> brandCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            if (skuInfoEntity == null) {
                return ;
            }
            Resp<BrandEntity> brandEntityResp = pmsClient.queryBrand(skuInfoEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResp.getData();
            if (brandEntity != null) {
                itemVo.setBrandId(skuInfoEntity.getBrandId());
                itemVo.setBrandName(brandEntity.getName());
            }
        },threadPool);

        CompletableFuture<Void> categoryCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            if (skuInfoEntity == null) {
                return ;
            }
            Resp<CategoryEntity> categoryEntityResp = pmsClient.queryCate(skuInfoEntity.getCatalogId());
            CategoryEntity categoryEntity = categoryEntityResp.getData();
            if (categoryEntity != null) {
                itemVo.setCategorytId(skuInfoEntity.getCatalogId());
                itemVo.setCategoryName(categoryEntity.getName());
            }
        },threadPool);


        CompletableFuture<Void> saleCompletableFuture = CompletableFuture.runAsync(() -> {
            Resp<List<ItemSaleVo>> saleVoResp = smsClient.querySaleVoBySkuId(skuId);
            List<ItemSaleVo> saleVos = saleVoResp.getData();
            itemVo.setItemSaleVo(saleVos);
        },threadPool);

        CompletableFuture<Void> groupCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            if (skuInfoEntity == null) {
                return ;
            }
            Resp<List<ItemGroupVo>> groupResp = pmsClient.queryItemGroupVo(skuInfoEntity.getCatalogId(), skuInfoEntity.getSpuId());
            List<ItemGroupVo> itemGroupVos = groupResp.getData();
            itemVo.setItemGroupVos(itemGroupVos);
        },threadPool);


        CompletableFuture<Void> attrCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfoEntity -> {
            if (skuInfoEntity == null) {
                return ;
            }
            Resp<List<SkuSaleAttrValueEntity>> salesResp = pmsClient.querySaleAttrBySpuId(skuInfoEntity.getSpuId());
            List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = salesResp.getData();
            itemVo.setSaleAttrs(skuSaleAttrValueEntities);
        },threadPool);

        CompletableFuture.allOf(wareCompletableFuture,imageCompletableFuture,spuCompletableFuture,descCompletableFuture,
                brandCompletableFuture,categoryCompletableFuture,saleCompletableFuture,groupCompletableFuture,attrCompletableFuture).join();

        return itemVo;
    }


    public static void main(String[] args) {

        CompletableFuture.supplyAsync(()->{
            System.out.println("一号飞行员已就位！");
            int i = 1;
            if (i==0){
                throw new RuntimeException("一号飞机----boom!!");
            }
            return "请一号小憨批发达指示";
        }).whenCompleteAsync((t,u)->{
            System.out.println("小憨批一号"+t);
            System.out.println("小憨批二号"+u);
        });
    }
}
