package com.smartrestock.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PurchaseOrderDraftDTO {

    private String supplierCode;
    private String supplierName;
    private String storeCode;
    private String storeName;
    private List<PurchaseOrderItemDraft> items;

    @Data
    public static class PurchaseOrderItemDraft {

        private String skuCode;
        private String productName;
        private String categoryCode;
        private Integer quantity;
        private BigDecimal purchasePrice;
        private BigDecimal amount;
    }
}
