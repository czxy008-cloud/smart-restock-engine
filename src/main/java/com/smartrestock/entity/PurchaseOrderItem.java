package com.smartrestock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_purchase_order_item")
public class PurchaseOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private String skuCode;

    private String productName;

    private String categoryCode;

    private Integer quantity;

    private BigDecimal purchasePrice;

    private BigDecimal amount;

    private Integer receivedQuantity;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
