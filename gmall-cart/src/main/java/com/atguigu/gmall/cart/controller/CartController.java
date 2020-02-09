package com.atguigu.gmall.cart.controller;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.cart.Pojo.Cart;
import com.atguigu.gmall.cart.interceptor.CartInterceptor;
import com.atguigu.gmall.cart.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping("test")
    public String test(HttpServletRequest request){
//        System.out.println(request.getAttribute("userKey"));
//        System.out.println(request.getAttribute("userId"));
        System.out.println(CartInterceptor.getUserInfo());
        return "ffsfe";
    }

    @PostMapping("insert")
    public Resp<Object> insert(@RequestBody Cart cart){
        cartService.insert(cart);
        return Resp.ok(null);
    }

    @GetMapping
    public Resp<List<Cart>> cart(){
        List<Cart> carts = cartService.cart();
        return Resp.ok(carts);
    }

    @PostMapping("update")
    public Resp<Object> updateCount(@RequestBody Cart cart){
        cartService.updateCount(cart);
        return Resp.ok(null);
    }

    @PostMapping("check")
    public Resp<Object> check(@RequestBody Cart cart){
        cartService.check(cart);
        return Resp.ok(null);
    }

    @PostMapping("delete")
    public Resp<Object> delete(@RequestParam("skuId")Long skuId){
        cartService.delete(skuId);
        return Resp.ok(null);
    }

    @GetMapping("{userId}")
    public Resp<List<Cart>> queryCheckedCartByUserId(@PathVariable("userId")Long userId){
        List<Cart> carts = cartService.queryCheckedCartByUserId(userId);
        return Resp.ok(carts);
    }

}
