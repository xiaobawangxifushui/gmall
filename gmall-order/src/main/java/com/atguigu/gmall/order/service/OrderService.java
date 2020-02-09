package com.atguigu.gmall.order.service;


import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.vo.OrderConfirmVo;

public interface OrderService {
    OrderConfirmVo comfirm();

    void submit(OrderSubmitVo orderSubmitVo);
}
