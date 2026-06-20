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
@TableName("t_sales_record")
public class SalesRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String skuCode;

    private String productName;

    private String storeCode;

    private String storeName;

    private String categoryCode;

    private LocalDate salesDate;

    private Integer salesQuantity;

    private BigDecimal salesAmount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
