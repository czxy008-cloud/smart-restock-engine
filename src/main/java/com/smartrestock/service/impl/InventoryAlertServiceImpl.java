package com.smartrestock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartrestock.dto.InventoryAlertDTO;
import com.smartrestock.entity.InventorySummary;
import com.smartrestock.entity.RestockRule;
import com.smartrestock.entity.SalesRecord;
import com.smartrestock.mapper.InventorySummaryMapper;
import com.smartrestock.mapper.ProductMapper;
import com.smartrestock.mapper.RestockRuleMapper;
import com.smartrestock.mapper.SalesRecordMapper;
import com.smartrestock.service.InventoryAlertService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class InventoryAlertServiceImpl implements InventoryAlertService {

    private final InventorySummaryMapper inventorySummaryMapper;
    private final SalesRecordMapper salesRecordMapper;
    private final RestockRuleMapper restockRuleMapper;
    private final ProductMapper productMapper;

    public InventoryAlertServiceImpl(InventorySummaryMapper inventorySummaryMapper,
                                     SalesRecordMapper salesRecordMapper,
                                     RestockRuleMapper restockRuleMapper,
                                     ProductMapper productMapper) {
        this.inventorySummaryMapper = inventorySummaryMapper;
        this.salesRecordMapper = salesRecordMapper;
        this.restockRuleMapper = restockRuleMapper;
        this.productMapper = productMapper;
    }

    @Override
    public List<InventoryAlertDTO> getAlertList(String storeCode, String categoryCode) {
        List<InventorySummary> alertItems = inventorySummaryMapper.selectAlertList(storeCode, categoryCode);
        List<InventoryAlertDTO> result = new ArrayList<>();
        for (InventorySummary summary : alertItems) {
            result.add(convertToDTO(summary));
        }
        return result;
    }

    @Override
    public InventoryAlertDTO getAlertDetail(String skuCode, String storeCode) {
        QueryWrapper<InventorySummary> wrapper = new QueryWrapper<>();
        wrapper.eq("sku_code", skuCode).eq("store_code", storeCode);
        InventorySummary summary = inventorySummaryMapper.selectOne(wrapper);
        if (summary == null) {
            return null;
        }
        return convertToDTO(summary);
    }

    @Override
    public void refreshAlertStatus() {
        List<InventorySummary> allSummaries = inventorySummaryMapper.selectList(null);
        for (InventorySummary summary : allSummaries) {
            QueryWrapper<RestockRule> ruleWrapper = new QueryWrapper<>();
            ruleWrapper.eq("sku_code", summary.getSkuCode()).eq("store_code", summary.getStoreCode());
            RestockRule rule = restockRuleMapper.selectOne(ruleWrapper);
            if (rule != null) {
                if (summary.getCurrentQuantity() <= rule.getReorderPoint() * 0.5) {
                    summary.setAlertStatus(2);
                } else if (summary.getCurrentQuantity() <= rule.getReorderPoint()) {
                    summary.setAlertStatus(1);
                } else {
                    summary.setAlertStatus(0);
                }
                List<SalesRecord> recentSales = salesRecordMapper.selectRecentSales(
                        summary.getSkuCode(), summary.getStoreCode(), rule.getMovingAvgWindow());
                int dailyAvg = 0;
                if (!recentSales.isEmpty()) {
                    int totalSales = 0;
                    for (SalesRecord record : recentSales) {
                        totalSales += record.getSalesQuantity();
                    }
                    dailyAvg = totalSales / recentSales.size();
                }
                summary.setSafetyQuantity(dailyAvg * rule.getSafetyStockDays());
            }
            inventorySummaryMapper.updateById(summary);
        }
    }

    private InventoryAlertDTO convertToDTO(InventorySummary summary) {
        InventoryAlertDTO dto = new InventoryAlertDTO();
        dto.setSkuCode(summary.getSkuCode());
        dto.setProductName(summary.getProductName());
        dto.setStoreCode(summary.getStoreCode());
        dto.setStoreName(summary.getStoreName());
        dto.setCategoryCode(summary.getCategoryCode());
        dto.setCurrentQuantity(summary.getCurrentQuantity());
        dto.setSafetyQuantity(summary.getSafetyQuantity());
        dto.setAlertStatus(summary.getAlertStatus());

        String alertLevel;
        switch (summary.getAlertStatus()) {
            case 2:
                alertLevel = "紧急";
                break;
            case 1:
                alertLevel = "预警";
                break;
            default:
                alertLevel = "正常";
                break;
        }
        dto.setAlertLevel(alertLevel);

        QueryWrapper<RestockRule> ruleWrapper = new QueryWrapper<>();
        ruleWrapper.eq("sku_code", summary.getSkuCode()).eq("store_code", summary.getStoreCode());
        RestockRule rule = restockRuleMapper.selectOne(ruleWrapper);

        int suggestedRestockQty = 0;
        if (rule != null) {
            List<SalesRecord> recentSales = salesRecordMapper.selectRecentSales(
                    summary.getSkuCode(), summary.getStoreCode(), rule.getMovingAvgWindow());
            int dailyAvg = 0;
            if (!recentSales.isEmpty()) {
                int totalSales = 0;
                for (SalesRecord record : recentSales) {
                    totalSales += record.getSalesQuantity();
                }
                dailyAvg = totalSales / recentSales.size();
            }
            suggestedRestockQty = (dailyAvg * rule.getLeadTimeDays() + rule.getReorderPoint()) - summary.getCurrentQuantity();
            if (suggestedRestockQty < rule.getMinOrderQuantity()) {
                suggestedRestockQty = rule.getMinOrderQuantity();
            }
            int multiple = rule.getOrderQuantityMultiple();
            suggestedRestockQty = ((suggestedRestockQty + multiple - 1) / multiple) * multiple;
            if (suggestedRestockQty < 0) {
                suggestedRestockQty = 0;
            }
        }
        dto.setSuggestedRestockQty(suggestedRestockQty);

        List<SalesRecord> last30DaysSales = salesRecordMapper.selectRecentSales(
                summary.getSkuCode(), summary.getStoreCode(), 30);
        int totalSales = 0;
        for (SalesRecord record : last30DaysSales) {
            totalSales += record.getSalesQuantity();
        }
        BigDecimal turnoverRate;
        if (summary.getCurrentQuantity() > 0) {
            turnoverRate = BigDecimal.valueOf(totalSales).divide(
                    BigDecimal.valueOf(summary.getCurrentQuantity()), 2, RoundingMode.HALF_UP);
        } else {
            turnoverRate = BigDecimal.ZERO;
        }
        dto.setTurnoverRate(turnoverRate);

        return dto;
    }
}
