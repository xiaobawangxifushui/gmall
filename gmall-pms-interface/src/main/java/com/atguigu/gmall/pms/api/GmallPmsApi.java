package com.atguigu.gmall.pms.api;

import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.CategoryVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface GmallPmsApi {
    @PostMapping("pms/spuinfo/page")
    public Resp<List<SpuInfoEntity>> querySkuInfoByPagr(@RequestBody QueryCondition condition);

    @GetMapping("pms/skuinfo/{spuId}")
    public Resp<List<SkuInfoEntity>> querySkuInfosBySpuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/category")
    public Resp<List<CategoryEntity>> getCategories(@RequestParam(value = "level",defaultValue = "0") Integer level
            ,@RequestParam(value = "parentCid",required = false) Long pid);

    @GetMapping("pms/category/{pid}")
    public Resp<List<CategoryVo>> queryCateWithSub(@PathVariable("pid")Long pid);

    @GetMapping("pms/spuinfo/info/{id}")
    public Resp<SpuInfoEntity> info(@PathVariable("id") Long id);

    @GetMapping("pms/brand/info/{brandId}")
    public Resp<BrandEntity> queryBrand(@PathVariable("brandId") Long brandId);

    @GetMapping("pms/category/info/{catId}")
    public Resp<CategoryEntity> queryCate(@PathVariable("catId") Long catId);

    @GetMapping("pms/productattrvalue/{spuId}")
    public Resp<List<ProductAttrValueEntity>> queryProBySkuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/skuinfo/info/{skuId}")
    public Resp<SkuInfoEntity> querySkuById(@PathVariable("skuId") Long skuId);

    @GetMapping("pms/skuimages/{skuId}")
    public Resp<List<SkuImagesEntity>>  queryImageBySkuId(@PathVariable("skuId")Long skuId);

    @GetMapping("pms/attrgroup/getGroup")
    public Resp<List<ItemGroupVo>> queryItemGroupVo(@RequestParam("cid")Long cid,
                                                    @RequestParam("spuId")Long spuId);

    @GetMapping("pms/skusaleattrvalue/{spuId}")
    public Resp<List<SkuSaleAttrValueEntity>> querySaleAttrBySpuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/spuinfodesc/info/{spuId}")
    public Resp<SpuInfoDescEntity> desc(@PathVariable("spuId") Long spuId);
}
