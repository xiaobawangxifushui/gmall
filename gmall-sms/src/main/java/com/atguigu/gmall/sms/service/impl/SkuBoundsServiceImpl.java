package com.atguigu.gmall.sms.service.impl;

import com.atguigu.gmall.sms.dao.SkuFullReductionDao;
import com.atguigu.gmall.sms.dao.SkuLadderDao;
import com.atguigu.gmall.sms.entity.SkuFullReductionEntity;
import com.atguigu.gmall.sms.entity.SkuLadderEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.sms.vo.SaleVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.sms.dao.SkuBoundsDao;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.sms.service.SkuBoundsService;
import org.springframework.transaction.annotation.Transactional;


@Service("skuBoundsService")
public class SkuBoundsServiceImpl extends ServiceImpl<SkuBoundsDao, SkuBoundsEntity> implements SkuBoundsService {
    @Autowired
    private SkuLadderDao skuLadderDao;
    @Autowired
    private SkuFullReductionDao reductionDao;
    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<SkuBoundsEntity> page = this.page(
                new Query<SkuBoundsEntity>().getPage(params),
                new QueryWrapper<SkuBoundsEntity>()
        );

        return new PageVo(page);
    }
    @Transactional
    @Override
    public void saveSale(SaleVo saleVo) {
        SkuBoundsEntity skuBoundsEntity = new SkuBoundsEntity();
        BeanUtils.copyProperties(saleVo,skuBoundsEntity);
        skuBoundsEntity.setSkuId(saleVo.getSkuId());
        List<String> work = saleVo.getWork();
        skuBoundsEntity.setWork(new Integer(work.get(0))+new Integer(work.get(1))*2+new Integer(work.get(2))*4+new Integer(work.get(3))*8);
        this.save(skuBoundsEntity);


        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        BeanUtils.copyProperties(saleVo,skuLadderEntity);
        skuLadderEntity.setAddOther(saleVo.getLadderAddOther());
        skuLadderEntity.setSkuId(saleVo.getSkuId());
        skuLadderDao.insert(skuLadderEntity);

        SkuFullReductionEntity reductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(saleVo,reductionEntity);
        reductionEntity.setAddOther(saleVo.getFullAddOther());
        reductionEntity.setSkuId(saleVo.getSkuId());
        reductionDao.insert(reductionEntity);

    }

    @Override
    public List<ItemSaleVo> querySaleVoBySkuId(Long skuId) {
        List<ItemSaleVo> saleVos = new ArrayList<>();
        ItemSaleVo bound = new ItemSaleVo();
        SkuBoundsEntity skuBoundsEntity = this.getOne(new QueryWrapper<SkuBoundsEntity>().eq("sku_id", skuId));
        if (skuBoundsEntity!=null){
            bound.setType("积分");
            bound.setDesc("赠送"+ skuBoundsEntity.getGrowBounds()+"成长积分,"+skuBoundsEntity.getBuyBounds()+"消费积分");
        }
        saleVos.add(bound);

        ItemSaleVo full = new ItemSaleVo();
        SkuFullReductionEntity fullReductionEntity = reductionDao.selectOne(new QueryWrapper<SkuFullReductionEntity>().eq("sku_id", skuId));
        if (fullReductionEntity!=null){
            full.setType("满减");
            full.setDesc("满"+fullReductionEntity.getFullPrice()+"减"+fullReductionEntity.getReducePrice());
        }
        saleVos.add(full);

        ItemSaleVo ladder = new ItemSaleVo();
        SkuLadderEntity skuLadderEntity = skuLadderDao.selectOne(new QueryWrapper<SkuLadderEntity>().eq("sku_id", skuId));
        if (skuBoundsEntity!=null){
            ladder.setType("打折");
            ladder.setDesc("满"+skuLadderEntity.getFullCount()+"件,打"+skuLadderEntity.getDiscount().divide(new BigDecimal(10))+"折");
        }
        saleVos.add(ladder);
        return saleVos;
    }

}