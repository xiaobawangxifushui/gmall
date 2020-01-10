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
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest
@RunWith(SpringRunner.class)
public class GmallSearchApplicationTests {
    @Autowired
    private RestHighLevelClient highLevelClient;
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
    public void test() {
        Long pageNo = 1l;
        Long pageSize = 100l;

        do {
            QueryCondition condition = new QueryCondition();
            condition.setPage(pageNo);
            condition.setLimit(pageSize);
            Resp<List<SpuInfoEntity>> spuInfoResp = pmsClient.querySkuInfoByPagr(condition);
            List<SpuInfoEntity> spuInfoEntities = spuInfoResp.getData();
            spuInfoEntities.forEach(spuInfoEntity -> {
                Resp<List<SkuInfoEntity>> skuResp = pmsClient.querySkuInfosBySpuId(spuInfoEntity.getId());
                List<SkuInfoEntity> skuInfoEntities = skuResp.getData();
                if (!CollectionUtils.isEmpty(skuInfoEntities)) {
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
                        if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                            goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
                        }
                        goods.setCreateTime(spuInfoEntity.getCreateTime());

                        Resp<BrandEntity> brandEntityResp = pmsClient.queryBrand(spuInfoEntity.getBrandId());
                        BrandEntity brandEntity = brandEntityResp.getData();
                        if (brandEntity != null) {
                            goods.setBrandId(spuInfoEntity.getBrandId());
                            goods.setBrandName(brandEntity.getName());
                        }

                        Resp<CategoryEntity> categoryEntityResp = pmsClient.queryCate(spuInfoEntity.getCatalogId());
                        CategoryEntity categoryEntity = categoryEntityResp.getData();
                        if (categoryEntity != null) {
                            goods.setCategoryId(spuInfoEntity.getCatalogId());
                            goods.setCategoryName(categoryEntity.getName());
                        }

                        Resp<List<ProductAttrValueEntity>> listResp = pmsClient.queryProBySkuId(spuInfoEntity.getId());
                        List<ProductAttrValueEntity> productAttrValueEntities = listResp.getData();
                        if (!CollectionUtils.isEmpty(productAttrValueEntities)) {
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
            pageSize = (long) spuInfoEntities.size();
            pageNo++;
        } while (pageSize == 100);

    }

    @Test
    public void test7() {
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(QueryBuilders.matchQuery("brandName", "时间简品"));
        queryBuilder.withPageable(PageRequest.of(0, 1000));
        restTemplate.query(queryBuilder.build(), r -> {
            SearchHit[] hits = r.getHits().getHits();
            int a = 0;
            for (SearchHit hit : hits
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
    public void test8() throws IOException {
        SearchSourceBuilder builder = new SearchSourceBuilder();
        builder.query(QueryBuilders.matchQuery("skuTitle", "小米手机").operator(Operator.AND));
        builder.sort("skuId", SortOrder.DESC);
        builder.from(0);
        builder.size(20);
        builder.highlighter(new HighlightBuilder().field("skuTitle").preTags("<em>").postTags("</em>"));
        builder.aggregation(AggregationBuilders.terms("brandAgg").field("brandName")
                .subAggregation(AggregationBuilders.avg("avgPrice").field("price")));
        SearchResponse response = highLevelClient.search(new SearchRequest(new String[]{"goods"}, builder), RequestOptions.DEFAULT);
        SearchHit[] hits = response.getHits().getHits();
        for (SearchHit hit : hits
                ) {
            String sourceAsString = hit.getSourceAsString();
            Goods goods = JSON.parseObject(sourceAsString, Goods.class);
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            HighlightField skuTitle = highlightFields.get("skuTitle");
            goods.setSkuTitle(skuTitle.getFragments()[0].toString());
            System.out.println(goods);
        }
        Map<String, Aggregation> stringAggregationMap = response.getAggregations().asMap();
        ParsedStringTerms brandAgg = (ParsedStringTerms) stringAggregationMap.get("brandAgg");
        brandAgg.getBuckets().forEach(b -> System.out.println(b.getKeyAsString()));
    }

    @Test
    public void test9() throws IOException {
        SearchSourceBuilder builder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.matchQuery("skuTitle", "小米手机").operator(Operator.AND));


        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        BoolQueryBuilder sub = QueryBuilders.boolQuery();
        sub.must(QueryBuilders.termQuery("attrs.attrId", "33"));
        sub.must(QueryBuilders.termsQuery("attrs.attrValue", "3000", "4000"));
        boolQuery.must(QueryBuilders.nestedQuery("attr", sub, ScoreMode.None));

        boolQuery.filter(QueryBuilders.termsQuery("brandId", "5"));
        boolQuery.filter(QueryBuilders.termsQuery("categoryId", "255"));

        boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").gte(1000).lte(7000));

        builder.query(boolQueryBuilder);
        builder.from(0);
        builder.size(10);
        builder.highlighter(new HighlightBuilder().field("skuTitle").preTags("<em>").postTags("</em>"));
        builder.sort("price", SortOrder.DESC);
        builder.aggregation(AggregationBuilders.nested("attr_agg", "attrs")
                .subAggregation(AggregationBuilders.terms("attr_id").field("attrs.attrId"))
                .subAggregation(AggregationBuilders.terms("attr_name").field("attrs.attrName"))
                .subAggregation(AggregationBuilders.terms("attr_value").field("attrs.attrValue")));
        builder.aggregation(AggregationBuilders.terms("brand_id").field("brandId")
                .subAggregation(AggregationBuilders.terms("brand_name").field("brandName")));
        builder.aggregation(AggregationBuilders.terms("categoty_id").field("categoryId")
                .subAggregation(AggregationBuilders.terms("category_name").field("categoryName")));
        SearchResponse response = highLevelClient.search(new SearchRequest(new String[]{"goods"}, builder), RequestOptions.DEFAULT);
        SearchHit[] hits = response.getHits().getHits();
        for (SearchHit hit : hits
                ) {
            String sourceAsString = hit.getSourceAsString();
            Goods goods = JSON.parseObject(sourceAsString, Goods.class);

            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            HighlightField skuTitle = highlightFields.get("skuTitle");
            goods.setSkuTitle(skuTitle.getFragments()[0].toString());
            System.out.println(goods);
        }
        Map<String, Aggregation> stringAggregationMap = response.getAggregations().asMap();
        ParsedNested brandId = (ParsedNested) stringAggregationMap.get("brand_id");


//        Map<String, Aggregation> asMap = brandId.getAggregations().getAsMap();
//        ParsedStringTerms brandName = (ParsedStringTerms)asMap.get("brand_name");
//        brandName.getBuckets().forEach(b-> System.out.println(b.getKeyAsString()));
//
    }

}
