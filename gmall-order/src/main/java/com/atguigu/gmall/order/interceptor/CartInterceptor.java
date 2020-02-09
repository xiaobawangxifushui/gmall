package com.atguigu.gmall.order.interceptor;


import com.atguigu.core.bean.UserInfo;
import com.atguigu.core.utils.CookieUtils;
import com.atguigu.core.utils.JwtUtils;

import com.atguigu.gmall.order.properties.AuthProperties;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

@Component
@EnableConfigurationProperties(AuthProperties.class)
public class CartInterceptor implements HandlerInterceptor{
    @Autowired
    private AuthProperties authProperties;

    private static ThreadLocal<UserInfo> THRED_LOCAL = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        UserInfo userInfo = new UserInfo();
        String token = CookieUtils.getCookieValue(request, authProperties.getCookieName());

        if (StringUtils.isEmpty(token)){
            return false;
        }

        try {
            Map<String, Object> infoFromToken = JwtUtils.getInfoFromToken(token, authProperties.getPublicKey());
            Long id = Long.valueOf(infoFromToken.get("id").toString()) ;
            userInfo.setUserId(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        THRED_LOCAL.set(userInfo);
        return true;
    }

    public static UserInfo getUserInfo(){
        return THRED_LOCAL.get();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        THRED_LOCAL.remove();
    }
}
