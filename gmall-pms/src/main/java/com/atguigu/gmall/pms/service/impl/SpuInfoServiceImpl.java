package com.atguigu.gmall.pms.service.impl;


import com.atguigu.gmall.pms.dao.AttrDao;
import com.atguigu.gmall.pms.dao.SkuInfoDao;
import com.atguigu.gmall.pms.dao.SpuInfoDescDao;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.service.*;
import com.atguigu.gmall.pms.vo.BaseAttrVo;
import com.atguigu.gmall.pms.vo.SkuInfoVo;
import com.atguigu.gmall.pms.vo.SpuInfoVo;
import com.atguigu.gmall.sms.vo.SaleVo;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.pms.dao.SpuInfoDao;
import org.springframework.util.CollectionUtils;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    private ProductAttrValueService productAttrValueService;
    @Autowired
    private SpuInfoDescDao descDao;
    @Autowired
    private SkuInfoDao skuInfoDao;
    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;
    @Autowired
    private SkuImagesService imagesService;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private AttrDao attrDao;
    @Autowired
    private SpuInfoDescService spuInfoDescService;
    @Autowired
    private AmqpTemplate amqpTemplate;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public PageVo querySpuPageVo(QueryCondition queryCondition, Long catId) {
        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();
        if(catId !=0){
            wrapper.eq("catalog_id",catId);
        }
        String key = queryCondition.getKey();
        if (!StringUtils.isEmpty(key)){
            wrapper.and(t -> t.eq("id",key).or().like("spu_name",key));
        }
        IPage<SpuInfoEntity> page = this.page(new Query<SpuInfoEntity>().getPage(queryCondition), wrapper);
        return new PageVo(page);
    }
    @GlobalTransactional
    @Override
    public void bigSave(SpuInfoVo spuInfoVo) {
        saveSpuInfoVo(spuInfoVo);
        Long spuId = spuInfoVo.getId();
        //System.out.println(spuId);

        saveBaseAttrs(spuInfoVo, spuId);

        List<String> images = saveSpuDesc(spuInfoVo, spuId);
        //List<String> images = spuInfoDescService.saveSpuDesc(spuInfoVo, spuId);
        saveSkus(spuInfoVo, spuId, images);

        amqpTemplate.convertAndSend("GMALL-PMS-EXCHANGE","iterm.insert",spuId);
        //int a = 1/0;

    }

    public void saveSkus(SpuInfoVo spuInfoVo, Long spuId, List<String> images) {
        List<SkuInfoVo> skus = spuInfoVo.getSkus();
        skus.stream().forEach(sku ->{
            List<String> skuImages = sku.getImages();
            if (!CollectionUtils.isEmpty(skuImages)){
                sku.setSkuDefaultImg(sku.getSkuDefaultImg() == null?skuImages.get(0):sku.getSkuDefaultImg());
            }
            sku.setCatalogId(spuInfoVo.getCatalogId());
            sku.setBrandId(spuInfoVo.getBrandId());
            sku.setSpuId(spuId);
            sku.setSkuCode(UUID.randomUUID().toString());
            skuInfoDao.insert(sku);
            Long skuId = sku.getSkuId();

            List<SkuSaleAttrValueEntity> saleAttrs = sku.getSaleAttrs();
            saleAttrs.forEach(sale ->{
                sale.setAttrName(attrDao.selectOne(new QueryWrapper<AttrEntity>().eq("attr_id",sale.getAttrId())).getAttrName());
                sale.setAttrSort(0);
                sale.setSkuId(skuId);
            });
            skuSaleAttrValueService.saveBatch(saleAttrs);

            List<SkuImagesEntity> collect = images.stream().map(image -> {
                SkuImagesEntity imagesEntity = new SkuImagesEntity();
                imagesEntity.setSkuId(skuId);
                imagesEntity.setImgSort(0);
                imagesEntity.setImgUrl(image);
                imagesEntity.setDefaultImg(StringUtils.equals(sku.getSkuDefaultImg(), image) ? 1 : 0);
                return imagesEntity;
            }).collect(Collectors.toList());
            imagesService.saveBatch(collect);
            SaleVo saleVo = new SaleVo();
            BeanUtils.copyProperties(sku,saleVo);
            saleVo.setSkuId(skuId);
            smsClient.saveSale(saleVo);

        });
    }

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

    public void saveBaseAttrs(SpuInfoVo spuInfoVo, Long spuId) {
        List<BaseAttrVo> baseAttrs = spuInfoVo.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)){
            List<ProductAttrValueEntity> collect = baseAttrs.stream().map(baseAttrVo -> {
                ProductAttrValueEntity productAttrValueEntity = new ProductAttrValueEntity();
                BeanUtils.copyProperties(baseAttrVo, productAttrValueEntity);
                productAttrValueEntity.setSpuId(spuId);
                productAttrValueEntity.setAttrSort(0);
                productAttrValueEntity.setQuickShow(1);
                return productAttrValueEntity;
            }).collect(Collectors.toList());
            productAttrValueService.saveBatch(collect);
        }
    }

    public void saveSpuInfoVo(SpuInfoVo spuInfoVo) {
        spuInfoVo.setCreateTime(new Date());
        spuInfoVo.setUodateTime(spuInfoVo.getCreateTime());
        this.save(spuInfoVo);
    }

}