package com.smartrestock.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseOrderDetailDTO {

    private String orderNo;
    private String supplierCode;
    private String supplierName;
    private String storeCode;
    private String storeName;
    private Integer orderStatus;
    private String orderStatusText;
    private BigDecimal totalAmount;
    private Integer totalQuantity;
    private LocalDate orderDate;
    private LocalDate expectedDate;
    private String buyer;
    private String remark;
    private LocalDateTime createdTime;
    private List<ItemDetail> items;

    @Data
    public static class ItemDetail {

        private Long id;
        private String skuCode;
        private String productName;
        private String categoryCode;
        private Integer quantity;
        private BigDecimal purchasePrice;
        private BigDecimal amount;
        private Integer receivedQuantity;
        private String remark;
    }
}
