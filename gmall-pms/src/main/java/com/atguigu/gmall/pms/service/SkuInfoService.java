package com.atguigu.gmall.pms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;

import java.util.List;


/**
 * sku信息
 *
 * @author lixianfeng
 * @email lxf@atguigu.com
 * @date 2020-01-01 14:46:53
 */
public interface SkuInfoService extends IService<SkuInfoEntity> {

    PageVo queryPage(QueryCondition params);

    List<SkuInfoEntity> querySkuInfosBySpuId(Long spuId);
}

