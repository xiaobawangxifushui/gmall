package com.atguigu.gmall.index.service;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVo;

import java.util.List;

public interface IndexService {
    List<CategoryEntity> queryLe1Category();


    List<CategoryVo> queryCateWithSub(Long pid);

    void testLock();
}
