package com.atguigu.gmall.search;


import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValue;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
@RunWith(SpringRunner.class)
public class GmallSearchApplicationTests {
	@Autowired
	private ElasticsearchRestTemplate restTemplate;
	@Autowired
	private GmallPmsClient pmsClient;
	@Autowired
	private GoodsRepostory goodsRepostory;
	@Autowired
	private GmallWmsClient wmsClient;

	@Test
	public void contextLoads() {
		restTemplate.createIndex(Goods.class);
		restTemplate.putMapping(Goods.class);
	}

	@Test
	public  void  test(){
		Long pageNo = 1l;
		Long pageSize = 100l;

		do{
			QueryCondition condition = new QueryCondition();
			condition.setPage(pageNo);
			condition.setLimit(pageSize);
			Resp<List<SpuInfoEntity>> spuInfoResp = pmsClient.querySkuInfoByPagr(condition);
			List<SpuInfoEntity> spuInfoEntities = spuInfoResp.getData();
			spuInfoEntities.forEach(spuInfoEntity -> {
				Resp<List<SkuInfoEntity>> skuResp = pmsClient.querySkuInfosBySpuId(spuInfoEntity.getId());
				List<SkuInfoEntity> skuInfoEntities = skuResp.getData();
				if (!CollectionUtils.isEmpty(skuInfoEntities)){
					List<Goods> goodsList = skuInfoEntities.stream().map(skuInfoEntity -> {
						Goods goods = new Goods();
						goods.setSkuId(skuInfoEntity.getSkuId());
						goods.setSkuTitle(skuInfoEntity.getSkuTitle());
						goods.setSkuSubTitle(skuInfoEntity.getSkuSubtitle());
						goods.setPrice(skuInfoEntity.getPrice().doubleValue());
						goods.setDefaultImage(skuInfoEntity.getSkuDefaultImg());

						goods.setSale(10000l);
						Resp<List<WareSkuEntity>> wareResp = wmsClient.qureyWareSkuBySkuId(skuInfoEntity.getSkuId());
						List<WareSkuEntity> wareSkuEntities = wareResp.getData();
						if (!CollectionUtils.isEmpty(wareSkuEntities)){
							goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock()>0));
						}
						goods.setCreateTime(spuInfoEntity.getCreateTime());

						Resp<BrandEntity> brandEntityResp = pmsClient.queryBrand(spuInfoEntity.getBrandId());
						BrandEntity brandEntity = brandEntityResp.getData();
						if (brandEntity!=null){
							goods.setBrandId(spuInfoEntity.getBrandId());
							goods.setBrandName(brandEntity.getName());
						}

						Resp<CategoryEntity> categoryEntityResp = pmsClient.queryCate(spuInfoEntity.getCatalogId());
						CategoryEntity categoryEntity = categoryEntityResp.getData();
						if (categoryEntity!=null){
							goods.setCategoryId(spuInfoEntity.getCatalogId());
							goods.setCategoryName(categoryEntity.getName());
						}

						Resp<List<ProductAttrValueEntity>> listResp = pmsClient.queryProBySkuId(spuInfoEntity.getId());
						List<ProductAttrValueEntity> productAttrValueEntities = listResp.getData();
						if (!CollectionUtils.isEmpty(productAttrValueEntities)){
							List<SearchAttrValue> searchAttrValues = productAttrValueEntities.stream().map(productAttrValueEntity -> {
								SearchAttrValue searchAttrValue = new SearchAttrValue();
								searchAttrValue.setAttrId(productAttrValueEntity.getAttrId());
								searchAttrValue.setAttrName(productAttrValueEntity.getAttrName());
								searchAttrValue.setAttrValue(productAttrValueEntity.getAttrValue());
								return searchAttrValue;
							}).collect(Collectors.toList());
							goods.setAttrs(searchAttrValues);
						}
						return goods;
					}).collect(Collectors.toList());
					goodsRepostory.saveAll(goodsList);
				}


			});
			pageSize = (long)spuInfoEntities.size();
			pageNo++;
		}while (pageSize==100);

	}
	@Test
	public void  test7(){
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		queryBuilder.withQuery(QueryBuilders.matchQuery("brandName","时间简品"));
		queryBuilder.withPageable(PageRequest.of(0,1000));
		restTemplate.query(queryBuilder.build(),r->{
			SearchHit[] hits = r.getHits().getHits();
			int a =0;
			for (SearchHit hit:hits
				 ) {
				String sourceAsString = hit.getSourceAsString();
				Goods goods = JSON.parseObject(sourceAsString, Goods.class);
				System.out.println(goods);
				a++;
			}
			System.out.println(a);
			return null;
		});
	}
	@Test
	public void name(){

	}
}
