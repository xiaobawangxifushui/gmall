package com.atguigu.gmall.wms.vo;

import lombok.Data;

@Data
public class SkuWareVo {
    private String orderToken;
    private Long skuId;
    private Integer count;
    private Boolean lock = false;
    private Long wareId;
}
