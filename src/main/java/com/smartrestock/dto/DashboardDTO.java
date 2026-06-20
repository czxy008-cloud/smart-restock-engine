package com.smartrestock.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardDTO {

    private String storeCode;
    private String storeName;
    private String categoryCode;
    private String categoryName;
    private BigDecimal turnoverRate;
    private Integer totalSalesQuantity;
    private Integer totalInventoryQuantity;
    private BigDecimal totalSalesAmount;
    private Integer alertCount;
    private Integer productCount;
}
