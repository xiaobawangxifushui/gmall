package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.vo.SpuInfoVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.pms.dao.SpuInfoDescDao;
import com.atguigu.gmall.pms.entity.SpuInfoDescEntity;
import com.atguigu.gmall.pms.service.SpuInfoDescService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("spuInfoDescService")
public class SpuInfoDescServiceImpl extends ServiceImpl<SpuInfoDescDao, SpuInfoDescEntity> implements SpuInfoDescService {
    @Autowired
    private SpuInfoDescDao descDao;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<SpuInfoDescEntity> page = this.page(
                new Query<SpuInfoDescEntity>().getPage(params),
                new QueryWrapper<SpuInfoDescEntity>()
        );

        return new PageVo(page);
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public List<String> saveSpuDesc(SpuInfoVo spuInfoVo, Long spuId) {
        List<String> images = spuInfoVo.getSpuImages();
        if (!CollectionUtils.isEmpty(images)){
            SpuInfoDescEntity spuInfoDescEntity = new SpuInfoDescEntity();
            spuInfoDescEntity.setDecript(StringUtils.join(images,","));
            spuInfoDescEntity.setSpuId(spuId);
            descDao.insert(spuInfoDescEntity);
        }
        return images;
    }

}