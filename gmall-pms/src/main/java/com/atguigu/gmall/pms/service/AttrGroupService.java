package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.AttrGroupVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;

import java.util.List;


/**
 * 属性分组
 *
 * @author lixianfeng
 * @email lxf@atguigu.com
 * @date 2020-01-01 14:46:53
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageVo queryPage(QueryCondition params);

    PageVo getGroupByCid(QueryCondition queryCondition, Long catId);

    AttrGroupVo queryGroupVo(Long gid);

    List<AttrGroupVo> queryAttrGroupVosByCid(Long catId);

    List<ItemGroupVo> queryItemGroupVo(Long cid, Long spuId);
}

