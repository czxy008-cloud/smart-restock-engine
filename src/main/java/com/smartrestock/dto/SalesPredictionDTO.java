package com.smartrestock.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class SalesPredictionDTO {

    private String skuCode;
    private String productName;
    private String storeCode;
    private String storeName;
    private String categoryCode;
    private Integer avgDailySales;
    private BigDecimal seasonalFactor;
    private Integer predictedDailySales;
    private List<DailyPrediction> dailyPredictions;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyPrediction {

        private LocalDate date;
        private Integer predictedQuantity;
    }
}
