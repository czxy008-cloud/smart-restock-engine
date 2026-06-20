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
@TableName("t_product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String skuCode;

    private String productName;

    private String categoryCode;

    private String categoryName;

    private String unit;

    private BigDecimal purchasePrice;

    private BigDecimal retailPrice;

    private String supplierCode;

    private String supplierName;

    private String storeCode;

    private String storeName;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
