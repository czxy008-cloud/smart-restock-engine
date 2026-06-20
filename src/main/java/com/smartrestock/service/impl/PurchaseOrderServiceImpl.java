package com.smartrestock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartrestock.dto.PurchaseOrderDraftDTO;
import com.smartrestock.dto.PurchaseOrderDraftDTO.PurchaseOrderItemDraft;
import com.smartrestock.entity.InventorySummary;
import com.smartrestock.entity.Product;
import com.smartrestock.entity.PurchaseOrder;
import com.smartrestock.entity.RestockRule;
import com.smartrestock.entity.SalesRecord;
import com.smartrestock.mapper.InventorySummaryMapper;
import com.smartrestock.mapper.ProductMapper;
import com.smartrestock.mapper.PurchaseOrderItemMapper;
import com.smartrestock.mapper.PurchaseOrderMapper;
import com.smartrestock.mapper.RestockRuleMapper;
import com.smartrestock.mapper.SalesRecordMapper;
import com.smartrestock.service.PurchaseOrderService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final InventorySummaryMapper inventorySummaryMapper;
    private final SalesRecordMapper salesRecordMapper;
    private final RestockRuleMapper restockRuleMapper;
    private final ProductMapper productMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;

    public PurchaseOrderServiceImpl(InventorySummaryMapper inventorySummaryMapper,
                                    SalesRecordMapper salesRecordMapper,
                                    RestockRuleMapper restockRuleMapper,
                                    ProductMapper productMapper,
                                    PurchaseOrderMapper purchaseOrderMapper,
                                    PurchaseOrderItemMapper purchaseOrderItemMapper) {
        this.inventorySummaryMapper = inventorySummaryMapper;
        this.salesRecordMapper = salesRecordMapper;
        this.restockRuleMapper = restockRuleMapper;
        this.productMapper = productMapper;
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseOrderItemMapper = purchaseOrderItemMapper;
    }

    @Override
    public List<PurchaseOrderDraftDTO> generateDrafts(String storeCode) {
        QueryWrapper<InventorySummary> alertWrapper = new QueryWrapper<>();
        alertWrapper.eq("store_code", storeCode).gt("alert_status", 0);
        List<InventorySummary> alertItems = inventorySummaryMapper.selectList(alertWrapper);

        Map<String, List<PurchaseOrderItemDraft>> supplierGroups = new HashMap<>();
        Map<String, Product> supplierFirstProduct = new HashMap<>();

        for (InventorySummary alert : alertItems) {
            String skuCode = alert.getSkuCode();

            QueryWrapper<Product> productWrapper = new QueryWrapper<>();
            productWrapper.eq("sku_code", skuCode).eq("store_code", storeCode);
            Product product = productMapper.selectOne(productWrapper);
            if (product == null) {
                continue;
            }

            QueryWrapper<RestockRule> ruleWrapper = new QueryWrapper<>();
            ruleWrapper.eq("sku_code", skuCode).eq("store_code", storeCode);
            RestockRule rule = restockRuleMapper.selectOne(ruleWrapper);
            if (rule == null) {
                continue;
            }

            List<SalesRecord> recentSales = salesRecordMapper.selectRecentSales(skuCode, storeCode, rule.getMovingAvgWindow());
            double dailyAvg;
            if (recentSales == null || recentSales.isEmpty()) {
                dailyAvg = 0;
            } else {
                long totalQuantity = 0;
                for (SalesRecord sr : recentSales) {
                    totalQuantity += sr.getSalesQuantity();
                }
                dailyAvg = (double) totalQuantity / recentSales.size();
            }

            int base = (int) (dailyAvg * (rule.getLeadTimeDays() + rule.getOrderCycleDays())) - alert.getCurrentQuantity();
            if (base < rule.getMinOrderQuantity()) {
                base = rule.getMinOrderQuantity();
            }
            base = ((base + rule.getOrderQuantityMultiple() - 1) / rule.getOrderQuantityMultiple()) * rule.getOrderQuantityMultiple();
            if (base < 0) {
                base = 0;
            }

            if (base == 0) {
                continue;
            }

            PurchaseOrderItemDraft itemDraft = new PurchaseOrderItemDraft();
            itemDraft.setSkuCode(skuCode);
            itemDraft.setProductName(product.getProductName());
            itemDraft.setCategoryCode(product.getCategoryCode());
            itemDraft.setQuantity(base);
            itemDraft.setPurchasePrice(product.getPurchasePrice());
            itemDraft.setAmount(product.getPurchasePrice().multiply(new BigDecimal(base)).setScale(2, RoundingMode.HALF_UP));

            String supplierCode = product.getSupplierCode();
            supplierGroups.computeIfAbsent(supplierCode, k -> new ArrayList<>()).add(itemDraft);
            supplierFirstProduct.putIfAbsent(supplierCode, product);
        }

        List<PurchaseOrderDraftDTO> drafts = new ArrayList<>();
        for (Map.Entry<String, List<PurchaseOrderItemDraft>> entry : supplierGroups.entrySet()) {
            String supplierCode = entry.getKey();
            List<PurchaseOrderItemDraft> items = entry.getValue();
            Product firstProduct = supplierFirstProduct.get(supplierCode);

            PurchaseOrderDraftDTO draft = new PurchaseOrderDraftDTO();
            draft.setSupplierCode(supplierCode);
            draft.setSupplierName(firstProduct.getSupplierName());
            draft.setStoreCode(storeCode);
            draft.setStoreName(firstProduct.getStoreName());
            draft.setItems(items);
            drafts.add(draft);
        }

        return drafts;
    }

    @Override
    public PurchaseOrder confirmOrder(String orderNo) {
        QueryWrapper<PurchaseOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("order_no", orderNo);
        PurchaseOrder order = purchaseOrderMapper.selectOne(wrapper);

        if (order != null && order.getOrderStatus() == 0) {
            order.setOrderStatus(1);
            order.setOrderDate(LocalDate.now());
            order.setExpectedDate(LocalDate.now().plusDays(3));
            purchaseOrderMapper.updateById(order);
        }

        return order;
    }
}
