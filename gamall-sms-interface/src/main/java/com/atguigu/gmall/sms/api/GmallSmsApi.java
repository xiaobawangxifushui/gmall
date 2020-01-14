package com.atguigu.gmall.sms.api;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.sms.vo.SaleVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface GmallSmsApi {
    @PostMapping("sms/skubounds/savesale")
    public Resp saveSale(@RequestBody SaleVo saleVo);
    @GetMapping("sms/skubounds/{skuId}")
    public Resp<List<ItemSaleVo>> querySaleVoBySkuId(@PathVariable("skuId")Long skuId);
}
