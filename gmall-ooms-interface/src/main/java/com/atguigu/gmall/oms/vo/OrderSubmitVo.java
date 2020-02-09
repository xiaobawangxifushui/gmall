package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderSubmitVo {

    private String orderToken;

    private BigDecimal totalPrice;

    private MemberReceiveAddressEntity address;

    private Integer payType;

    private String deliveryCompany;

    private List<OrderItemVo>  items;

    private Integer bounds;

    private Long userId;

}
