package com.smartrestock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartrestock.dto.SalesPredictionDTO;
import com.smartrestock.entity.Product;
import com.smartrestock.entity.RestockRule;
import com.smartrestock.entity.SalesRecord;
import com.smartrestock.mapper.ProductMapper;
import com.smartrestock.mapper.RestockRuleMapper;
import com.smartrestock.mapper.SalesRecordMapper;
import com.smartrestock.service.SalesPredictionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SalesPredictionServiceImpl implements SalesPredictionService {

    private final SalesRecordMapper salesRecordMapper;
    private final RestockRuleMapper restockRuleMapper;
    private final ProductMapper productMapper;

    public SalesPredictionServiceImpl(SalesRecordMapper salesRecordMapper,
                                      RestockRuleMapper restockRuleMapper,
                                      ProductMapper productMapper) {
        this.salesRecordMapper = salesRecordMapper;
        this.restockRuleMapper = restockRuleMapper;
        this.productMapper = productMapper;
    }

    @Override
    public SalesPredictionDTO predict(String skuCode, String storeCode) {
        int movingAvgWindow = 14;
        BigDecimal seasonalFactor = BigDecimal.ONE;
        Integer seasonalFactorMonth = null;

        QueryWrapper<RestockRule> ruleWrapper = new QueryWrapper<>();
        ruleWrapper.eq("sku_code", skuCode).eq("store_code", storeCode);
        RestockRule rule = restockRuleMapper.selectOne(ruleWrapper);
        if (rule != null) {
            movingAvgWindow = rule.getMovingAvgWindow();
            seasonalFactor = rule.getSeasonalFactor();
            seasonalFactorMonth = rule.getSeasonalFactorMonth();
        }

        List<SalesRecord> salesRecords = salesRecordMapper.selectRecentSales(skuCode, storeCode, movingAvgWindow);

        int avgDailySales;
        if (salesRecords.isEmpty()) {
            avgDailySales = 0;
        } else {
            int totalSales = 0;
            for (SalesRecord record : salesRecords) {
                totalSales += record.getSalesQuantity();
            }
            avgDailySales = (int) Math.round((double) totalSales / salesRecords.size());
        }

        BigDecimal effectiveSeasonalFactor = BigDecimal.ONE;
        if (seasonalFactorMonth != null && seasonalFactorMonth.intValue() == LocalDate.now().getMonthValue()) {
            effectiveSeasonalFactor = seasonalFactor;
        }

        int predictedDailySales = (int) Math.round(avgDailySales * effectiveSeasonalFactor.doubleValue());

        List<SalesPredictionDTO.DailyPrediction> dailyPredictions = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            LocalDate date = LocalDate.now().plusDays(i);
            dailyPredictions.add(new SalesPredictionDTO.DailyPrediction(date, predictedDailySales));
        }

        QueryWrapper<Product> productWrapper = new QueryWrapper<>();
        productWrapper.eq("sku_code", skuCode).eq("store_code", storeCode);
        Product product = productMapper.selectOne(productWrapper);

        SalesPredictionDTO dto = new SalesPredictionDTO();
        dto.setSkuCode(skuCode);
        dto.setStoreCode(storeCode);
        dto.setAvgDailySales(avgDailySales);
        dto.setSeasonalFactor(effectiveSeasonalFactor);
        dto.setPredictedDailySales(predictedDailySales);
        dto.setDailyPredictions(dailyPredictions);

        if (product != null) {
            dto.setProductName(product.getProductName());
            dto.setStoreName(product.getStoreName());
            dto.setCategoryCode(product.getCategoryCode());
        }

        return dto;
    }

    @Override
    public List<SalesPredictionDTO> batchPredict(String storeCode, String categoryCode) {
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.eq("store_code", storeCode);
        if (categoryCode != null && !categoryCode.isEmpty()) {
            wrapper.eq("category_code", categoryCode);
        }
        List<Product> products = productMapper.selectList(wrapper);

        List<SalesPredictionDTO> results = new ArrayList<>();
        for (Product product : products) {
            results.add(predict(product.getSkuCode(), storeCode));
        }
        return results;
    }
}
