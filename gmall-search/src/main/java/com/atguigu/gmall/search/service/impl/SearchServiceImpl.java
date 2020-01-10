package com.atguigu.gmall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParem;
import com.atguigu.gmall.search.pojo.SearchResponseAttrVO;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import com.atguigu.gmall.search.service.SearchService;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    private RestHighLevelClient client;

    @Override
    public SearchResponseVo search(SearchParem searchParem) throws IOException {
        SearchSourceBuilder builder = builderDSL(searchParem);
        SearchResponse response = client.search(new SearchRequest(new String[]{"goods"}, builder), RequestOptions.DEFAULT);
        SearchResponseVo result = getResult(response);
        result.setPageNum(searchParem.getPageNum());
        result.setPageSize(searchParem.getPageSize());
        return result;
    }

    private SearchResponseVo getResult(SearchResponse response) {
        SearchResponseVo responseVo = new SearchResponseVo();
        SearchHits hits = response.getHits();
        if (hits != null) {
            SearchHit[] attrhits = hits.getHits();
            List<Goods> goodsList = new ArrayList<>();
            for (SearchHit hit : attrhits) {
                String sourceAsString = hit.getSourceAsString();
                Goods goods = JSON.parseObject(sourceAsString, Goods.class);

                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                HighlightField skuTitle = highlightFields.get("skuTitle");
                goods.setSkuTitle(skuTitle.getFragments()[0].string());
                goodsList.add(goods);
            }
            responseVo.setProducts(goodsList);
        }

        SearchResponseAttrVO brandVo = new SearchResponseAttrVO();
        brandVo.setProductAttributeId(null);
        brandVo.setName("品牌");
        Map<String, Aggregation> aggMap = response.getAggregations().asMap();
        ParsedLongTerms brandIdAgg = (ParsedLongTerms) aggMap.get("brandIdAgg");
        List<? extends Terms.Bucket> brandBuckets = brandIdAgg.getBuckets();
        List<String> brands = brandBuckets.stream().map(bucket -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", bucket.getKeyAsNumber());
            Map<String, Aggregation> brandName = bucket.getAggregations().asMap();
            ParsedStringTerms brandNameAgg = (ParsedStringTerms) brandName.get("brandNameAgg");
            String keyAsString = brandNameAgg.getBuckets().get(0).getKeyAsString();
            map.put("value", keyAsString);
            return JSON.toJSONString(map);
        }).collect(Collectors.toList());
        brandVo.setValue(brands);
        responseVo.setBrand(brandVo);

        SearchResponseAttrVO categoryVo = new SearchResponseAttrVO();
        categoryVo.setProductAttributeId(null);
        categoryVo.setName("分类");
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms) aggMap.get("categoryIdAgg");
        List<? extends Terms.Bucket> categoryBuckets = categoryIdAgg.getBuckets();
        List<String> categorys = categoryBuckets.stream().map(bucket -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", bucket.getKeyAsNumber());
            Map<String, Aggregation> asMap = bucket.getAggregations().asMap();
            ParsedStringTerms categoryNameAgg = (ParsedStringTerms) asMap.get("categoryNameAgg");
            map.put("value", categoryNameAgg.getBuckets().get(0).getKeyAsString());

            return JSON.toJSONString(map);
        }).collect(Collectors.toList());
        categoryVo.setValue(categorys);
        responseVo.setCatelog(categoryVo);

        ParsedNested attrsAgg = (ParsedNested) aggMap.get("attrsAgg");
        ParsedLongTerms attrIdAgg = attrsAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> attrIdBuckets = attrIdAgg.getBuckets();
        List<SearchResponseAttrVO> attrVOS = attrIdBuckets.stream().map(bucket -> {
            SearchResponseAttrVO attrVO = new SearchResponseAttrVO();
            attrVO.setProductAttributeId(bucket.getKeyAsNumber().longValue());
            Map<String, Aggregation> attrMap = bucket.getAggregations().asMap();
            ParsedStringTerms attrNameAgg = (ParsedStringTerms) attrMap.get("attrNameAgg");
            attrVO.setName(attrNameAgg.getBuckets().get(0).getKeyAsString());
            ParsedStringTerms attrValueAgg = (ParsedStringTerms) attrMap.get("attrValueAgg");
            List<String> collect = attrValueAgg.getBuckets().stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
            attrVO.setValue(collect);
            return attrVO;
        }).collect(Collectors.toList());


        responseVo.setAttrs(attrVOS);


        responseVo.setTotal(hits.getTotalHits());

        return responseVo;
    }

    private SearchSourceBuilder builderDSL(SearchParem searchParem) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        sourceBuilder.fetchSource(new String[]{"skuTitle", "skuSubTitle", "defaultImage", "price", "skuId"}, null);
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        String keyWord = searchParem.getKeyWord();
        if (StringUtils.isEmpty(keyWord)) {
            return null;
        }
        boolQueryBuilder.must(QueryBuilders.matchQuery("skuTitle", keyWord).operator(Operator.AND));

        Long[] brand = searchParem.getBrand();
        if (brand != null && brand.length != 0) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", brand));
        }

        Long[] catelog3 = searchParem.getCatelog3();
        if (catelog3 != null && catelog3.length != 0) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId", catelog3));
        }
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price");
        Double priceFrom = searchParem.getPriceFrom();
        if (priceFrom != null) {
            rangeQueryBuilder.gte(priceFrom);
        }
        Double priceTo = searchParem.getPriceTo();
        if (priceTo != null) {
            rangeQueryBuilder.lte(priceTo);
        }
        boolQueryBuilder.filter(rangeQueryBuilder);

        List<String> props = searchParem.getProps();
        props.forEach(prop -> {
            String[] attr = StringUtils.split(prop, ":");
            if (attr != null && attr.length == 2) {
                String attrId = attr[0];
                String[] attrValues = StringUtils.split(attr[1], "-");
                BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
                queryBuilder.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                queryBuilder.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                boolQueryBuilder.filter(QueryBuilders.nestedQuery("attrs", queryBuilder, ScoreMode.None));
            }
        });
        sourceBuilder.query(boolQueryBuilder);

        String order = searchParem.getOrder();
        if (!StringUtils.isEmpty(order)) {
            String[] oders = StringUtils.split(order, ":");
            if (oders != null && oders.length == 2) {
                String filedId = oders[0];
                String orderBy = oders[1];
                switch (filedId) {
                    case "0":
                        filedId = "_score";
                        break;
                    case "1":
                        filedId = "sale";
                        break;
                    case "2":
                        filedId = "price";
                        break;
                }
                sourceBuilder.sort(filedId, "asc".equals(orderBy) ? SortOrder.ASC : SortOrder.DESC);
            }
        }

        sourceBuilder.highlighter(new HighlightBuilder().field("skuTitle").preTags("<span style='corlor:red;'>").postTags("<span>"));
        Integer pageNum = searchParem.getPageNum();
        Integer pageSize = searchParem.getPageSize();
        if (pageNum > 0) {
            sourceBuilder.from(pageSize * (pageNum - 1));
        }
        sourceBuilder.size(pageSize);

        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName")));

        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));


        sourceBuilder.aggregation(AggregationBuilders.nested("attrsAgg", "attrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))
                ));


        //System.out.println(sourceBuilder);
        return sourceBuilder;
    }
}
