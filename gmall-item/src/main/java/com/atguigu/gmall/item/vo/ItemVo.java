package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.pms.entity.SpuInfoDescEntity;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ItemVo {

    private Long skuId;
    private String skuTitle;
    private String skuSubTitle;
    private BigDecimal price;
    private BigDecimal weight;

    private Boolean stock;
    private List<SkuImagesEntity> images;

    private Long spuId;
    private String spuName;
    private SpuInfoDescEntity desc;
    private Long brandId;
    private String brandName;
    private Long categorytId;
    private String categoryName;

    private List<ItemSaleVo> itemSaleVo;

    private List<SkuSaleAttrValueEntity> saleAttrs;

    private List<ItemGroupVo>  itemGroupVos;



}
