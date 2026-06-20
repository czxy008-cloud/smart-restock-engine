package com.smartrestock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_inventory_transaction")
public class InventoryTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String transactionNo;

    private String skuCode;

    private String storeCode;

    private Integer transactionType;

    private Integer quantity;

    private Integer beforeQuantity;

    private Integer afterQuantity;

    private String referenceNo;

    private String operator;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
