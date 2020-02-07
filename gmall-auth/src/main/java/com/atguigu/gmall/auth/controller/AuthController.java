package com.atguigu.gmall.auth.controller;

import com.atguigu.core.bean.Resp;
import com.atguigu.core.utils.CookieUtils;
import com.atguigu.gmall.auth.properties.AuthProperties;
import com.atguigu.gmall.auth.service.AuthService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("auth")
public class AuthController {
    @Autowired
    private AuthService authService;
    @Autowired
    private AuthProperties authProperties;

    @PostMapping("accredit")
    public Resp<Object> accredit(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            HttpServletRequest request,
            HttpServletResponse response
    ){
        String jwt = authService.accredit(username,password);
        if (StringUtils.isNotBlank(jwt)){
            CookieUtils.setCookie(request,response,authProperties.getCookieName(),jwt,authProperties.getExpire()*60*7200);
        }
        return Resp.ok(null);
    }


}
