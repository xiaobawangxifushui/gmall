package com.atguigu.gmall.cart.api;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.cart.Pojo.Cart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface GmallCartApi {
    @GetMapping("cart/{userId}")
    public Resp<List<Cart>> queryCheckedCartByUserId(@PathVariable("userId")Long userId);
}
