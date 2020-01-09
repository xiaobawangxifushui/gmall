package com.atguigu.gmall.search;

import com.atguigu.gmall.search.pojo.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface GoodsRepostory extends ElasticsearchRepository<Goods,Long>{
}
