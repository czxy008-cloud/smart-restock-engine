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
@TableName("t_restock_rule")
public class RestockRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String skuCode;

    private String storeCode;

    private Integer safetyStockDays;

    private Integer reorderPoint;

    private Integer maxStockDays;

    private Integer orderCycleDays;

    private Integer movingAvgWindow;

    private BigDecimal seasonalFactor;

    private Integer seasonalFactorMonth;

    private Integer minOrderQuantity;

    private Integer orderQuantityMultiple;

    private Integer leadTimeDays;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
