package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.cart.Pojo.Cart;

import java.util.List;

public interface CartService {
    void insert(Cart cart);

    List<Cart> cart();

    void updateCount(Cart cart);

    void check(Cart cart);

    void delete(Long skuId);
}
