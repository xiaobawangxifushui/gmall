package com.atguigu.gmall.search.service;

import com.atguigu.gmall.search.pojo.SearchParem;
import com.atguigu.gmall.search.pojo.SearchResponseVo;

import java.io.IOException;

public interface SearchService {
    SearchResponseVo search(SearchParem searchParem) throws IOException;
}
