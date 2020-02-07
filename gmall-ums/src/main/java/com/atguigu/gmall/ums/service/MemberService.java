package com.atguigu.gmall.ums.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;


/**
 * 会员
 *
 * @author lixianfeng
 * @email lxf@atguigu.com
 * @date 2019-12-31 13:40:47
 */
public interface MemberService extends IService<MemberEntity> {

    PageVo queryPage(QueryCondition params);

    Boolean checkData(String data, Integer type);

    void regist(MemberEntity memberEntity, String code);

    MemberEntity queryMember(String username, String password);
}

