package com.smartrestock.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryAlertDTO {

    private String skuCode;
    private String productName;
    private String storeCode;
    private String storeName;
    private String categoryCode;
    private Integer currentQuantity;
    private Integer safetyQuantity;
    private Integer alertStatus;
    private String alertLevel;
    private Integer suggestedRestockQty;
    private BigDecimal turnoverRate;
}
