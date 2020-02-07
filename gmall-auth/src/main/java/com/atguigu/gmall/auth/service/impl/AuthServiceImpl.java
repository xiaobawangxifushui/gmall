package com.atguigu.gmall.auth.service.impl;

import com.atguigu.core.bean.Resp;
import com.atguigu.core.exception.UmsException;
import com.atguigu.core.utils.JwtUtils;
import com.atguigu.gmall.auth.feign.GmallUmsClient;
import com.atguigu.gmall.auth.properties.AuthProperties;
import com.atguigu.gmall.auth.service.AuthService;
import com.atguigu.gmall.ums.entity.MemberEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@EnableConfigurationProperties(AuthProperties.class)
@Service
public class AuthServiceImpl implements AuthService{
    @Autowired
    private GmallUmsClient umsClient;
    @Autowired
    private AuthProperties authProperties;

    @Override
    public String accredit(String username, String password) {
        try {
            Resp<MemberEntity> memberEntityResp = umsClient.queryMember(username, password);
            MemberEntity memberEntity = memberEntityResp.getData();
            if (memberEntity==null){
                throw  new UmsException("用户名密码错误！");
            }
            Map<String, Object> map = new HashMap<>();
            map.put("id",memberEntity.getId());
            map.put("userName",memberEntity.getUsername());
            return JwtUtils.generateToken(map, authProperties.getPrivateKey(), authProperties.getExpire());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
