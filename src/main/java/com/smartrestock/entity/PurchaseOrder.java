package com.smartrestock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_purchase_order")
public class PurchaseOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private String supplierCode;

    private String supplierName;

    private String storeCode;

    private String storeName;

    private Integer orderStatus;

    private BigDecimal totalAmount;

    private Integer totalQuantity;

    private LocalDate orderDate;

    private LocalDate expectedDate;

    private String buyer;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
