package com.smartrestock.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartrestock.dto.CommonResult;
import com.smartrestock.dto.DashboardDTO;
import com.smartrestock.entity.InventorySummary;
import com.smartrestock.entity.Product;
import com.smartrestock.entity.SalesRecord;
import com.smartrestock.mapper.InventorySummaryMapper;
import com.smartrestock.mapper.ProductMapper;
import com.smartrestock.mapper.SalesRecordMapper;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final InventorySummaryMapper inventorySummaryMapper;
    private final SalesRecordMapper salesRecordMapper;
    private final ProductMapper productMapper;

    public DashboardController(InventorySummaryMapper inventorySummaryMapper,
                               SalesRecordMapper salesRecordMapper,
                               ProductMapper productMapper) {
        this.inventorySummaryMapper = inventorySummaryMapper;
        this.salesRecordMapper = salesRecordMapper;
        this.productMapper = productMapper;
    }

    @GetMapping("/turnover")
    public CommonResult<List<DashboardDTO>> turnover(@RequestParam(required = false) String storeCode,
                                                      @RequestParam(required = false) String categoryCode) {
        QueryWrapper<Product> productQuery = new QueryWrapper<>();
        if (storeCode != null) {
            productQuery.eq("store_code", storeCode);
        }
        if (categoryCode != null) {
            productQuery.eq("category_code", categoryCode);
        }
        List<Product> products = productMapper.selectList(productQuery);

        Map<String, Map<String, List<Product>>> grouped = products.stream()
                .collect(Collectors.groupingBy(
                        Product::getStoreCode,
                        HashMap::new,
                        Collectors.groupingBy(
                                Product::getCategoryCode,
                                HashMap::new,
                                Collectors.toList()
                        )
                ));

        List<DashboardDTO> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<Product>>> storeEntry : grouped.entrySet()) {
            String currentStoreCode = storeEntry.getKey();
            for (Map.Entry<String, List<Product>> categoryEntry : storeEntry.getValue().entrySet()) {
                String currentCategoryCode = categoryEntry.getKey();
                List<Product> groupProducts = categoryEntry.getValue();

                List<String> skuCodeList = groupProducts.stream()
                        .map(Product::getSkuCode)
                        .collect(Collectors.toList());

                if (skuCodeList.isEmpty()) {
                    continue;
                }

                QueryWrapper<SalesRecord> salesQuery = new QueryWrapper<>();
                salesQuery.ge("sales_date", LocalDate.now().minusDays(30));
                salesQuery.eq("store_code", currentStoreCode);
                salesQuery.in("sku_code", skuCodeList);
                List<SalesRecord> salesRecords = salesRecordMapper.selectList(salesQuery);

                int totalSalesQuantity = salesRecords.stream()
                        .mapToInt(SalesRecord::getSalesQuantity)
                        .sum();
                BigDecimal totalSalesAmount = salesRecords.stream()
                        .map(SalesRecord::getSalesAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                QueryWrapper<InventorySummary> inventoryQuery = new QueryWrapper<>();
                inventoryQuery.eq("store_code", currentStoreCode);
                inventoryQuery.in("sku_code", skuCodeList);
                List<InventorySummary> inventoryRecords = inventorySummaryMapper.selectList(inventoryQuery);

                int totalInventoryQuantity = inventoryRecords.stream()
                        .mapToInt(InventorySummary::getCurrentQuantity)
                        .sum();
                int alertCount = (int) inventoryRecords.stream()
                        .filter(i -> i.getAlertStatus() > 0)
                        .count();

                BigDecimal turnoverRate = totalInventoryQuantity > 0
                        ? BigDecimal.valueOf(totalSalesQuantity).divide(BigDecimal.valueOf(totalInventoryQuantity), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                DashboardDTO dto = new DashboardDTO();
                dto.setStoreCode(currentStoreCode);
                dto.setStoreName(groupProducts.get(0).getStoreName());
                dto.setCategoryCode(currentCategoryCode);
                dto.setCategoryName(groupProducts.get(0).getCategoryName());
                dto.setTurnoverRate(turnoverRate);
                dto.setTotalSalesQuantity(totalSalesQuantity);
                dto.setTotalInventoryQuantity(totalInventoryQuantity);
                dto.setTotalSalesAmount(totalSalesAmount);
                dto.setAlertCount(alertCount);
                dto.setProductCount(skuCodeList.size());

                result.add(dto);
            }
        }

        return CommonResult.success(result);
    }
}
