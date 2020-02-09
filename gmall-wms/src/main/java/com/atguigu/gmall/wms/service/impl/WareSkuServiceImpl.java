package com.atguigu.gmall.wms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.vo.SkuWareVo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.wms.dao.WareSkuDao;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.service.WareSkuService;
import org.springframework.util.CollectionUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private WareSkuDao wareSkuDao;

    private static final String WARE_PREFIX = "ware:unlock:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public List<SkuWareVo> checkAndLock(List<SkuWareVo> skuWareVos) {

        if (!CollectionUtils.isEmpty(skuWareVos)){
            skuWareVos.forEach(skuWareVo -> this.checkLock(skuWareVo));
        }

        if (skuWareVos.stream().anyMatch(skuWareVo -> !skuWareVo.getLock())){
            skuWareVos.forEach(skuWareVo -> {
                if (skuWareVo.getLock()){
                    wareSkuDao.rollBack(skuWareVo.getWareId(),skuWareVo.getCount());
                }
            });
            return skuWareVos;
        }
        String orderToken = skuWareVos.get(0).getOrderToken();
        redisTemplate.opsForValue().set(WARE_PREFIX + orderToken, JSON.toJSONString(skuWareVos));
        return null;
    }

    private void checkLock(SkuWareVo skuWareVo){
        RLock fairLock = redissonClient.getFairLock("lock" + skuWareVo.getSkuId());
        fairLock.lock();
        List<WareSkuEntity> wareSkuEntities = wareSkuDao.check(skuWareVo.getSkuId(),skuWareVo.getCount());
        if (!CollectionUtils.isEmpty(wareSkuEntities)){
            WareSkuEntity wareSkuEntity = wareSkuEntities.get(0);
            int flag = wareSkuDao.lock(wareSkuEntity.getId(),skuWareVo.getCount());
            if (flag!=0){
                skuWareVo.setLock(true);
                skuWareVo.setWareId(wareSkuEntity.getId());
            }
        }
        fairLock.unlock();
    }

}