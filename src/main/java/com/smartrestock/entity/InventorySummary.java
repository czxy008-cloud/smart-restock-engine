package com.smartrestock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_inventory_summary")
public class InventorySummary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String skuCode;

    private String productName;

    private String storeCode;

    private String storeName;

    private String categoryCode;

    private Integer currentQuantity;

    private Integer safetyQuantity;

    private Integer alertStatus;

    private LocalDate lastRestockDate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
