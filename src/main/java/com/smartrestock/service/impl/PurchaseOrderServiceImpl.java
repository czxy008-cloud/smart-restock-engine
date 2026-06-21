package com.smartrestock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartrestock.dto.PurchaseOrderDetailDTO;
import com.smartrestock.dto.PurchaseOrderDraftDTO;
import com.smartrestock.dto.PurchaseOrderDraftDTO.PurchaseOrderItemDraft;
import com.smartrestock.entity.InventorySummary;
import com.smartrestock.entity.Product;
import com.smartrestock.entity.PurchaseOrder;
import com.smartrestock.entity.PurchaseOrderItem;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        QueryWrapper<PurchaseOrder> pendingOrderWrapper = new QueryWrapper<>();
        pendingOrderWrapper.eq("store_code", storeCode).in("order_status", 0, 1, 2);
        List<PurchaseOrder> pendingOrders = purchaseOrderMapper.selectList(pendingOrderWrapper);
        Set<String> orderedSkuCodes = new HashSet<>();
        for (PurchaseOrder po : pendingOrders) {
            QueryWrapper<PurchaseOrderItem> itemWrapper = new QueryWrapper<>();
            itemWrapper.eq("order_no", po.getOrderNo());
            List<PurchaseOrderItem> items = purchaseOrderItemMapper.selectList(itemWrapper);
            for (PurchaseOrderItem item : items) {
                orderedSkuCodes.add(item.getSkuCode());
            }
        }

        QueryWrapper<InventorySummary> alertWrapper = new QueryWrapper<>();
        alertWrapper.eq("store_code", storeCode).gt("alert_status", 0);
        List<InventorySummary> alertItems = inventorySummaryMapper.selectList(alertWrapper);

        Map<String, List<PurchaseOrderItemDraft>> supplierGroups = new HashMap<>();
        Map<String, Product> supplierFirstProduct = new HashMap<>();

        for (InventorySummary alert : alertItems) {
            String skuCode = alert.getSkuCode();
            if (orderedSkuCodes.contains(skuCode)) {
                continue;
            }

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

    @Override
    public PurchaseOrder createPurchaseOrderFromDraft(PurchaseOrderDraftDTO draftDTO) {
        String orderNo = "PO" + System.currentTimeMillis() + (int)(Math.random() * 1000);

        List<PurchaseOrderItemDraft> items = draftDTO.getItems();
        int totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PurchaseOrderItemDraft item : items) {
            totalQuantity += item.getQuantity();
            totalAmount = totalAmount.add(item.getAmount());
        }
        totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);

        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNo(orderNo);
        order.setSupplierCode(draftDTO.getSupplierCode());
        order.setSupplierName(draftDTO.getSupplierName());
        order.setStoreCode(draftDTO.getStoreCode());
        order.setStoreName(draftDTO.getStoreName());
        order.setOrderStatus(0);
        order.setTotalAmount(totalAmount);
        order.setTotalQuantity(totalQuantity);
        order.setOrderDate(LocalDate.now());
        order.setBuyer("系统自动生成");
        purchaseOrderMapper.insert(order);

        for (PurchaseOrderItemDraft itemDraft : items) {
            com.smartrestock.entity.PurchaseOrderItem item = new com.smartrestock.entity.PurchaseOrderItem();
            item.setOrderNo(orderNo);
            item.setSkuCode(itemDraft.getSkuCode());
            item.setProductName(itemDraft.getProductName());
            item.setCategoryCode(itemDraft.getCategoryCode());
            item.setQuantity(itemDraft.getQuantity());
            item.setPurchasePrice(itemDraft.getPurchasePrice());
            item.setAmount(itemDraft.getAmount());
            item.setReceivedQuantity(0);
            purchaseOrderItemMapper.insert(item);
        }

        return order;
    }

    private String getOrderStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "草稿";
            case 1: return "已提交";
            case 2: return "部分到货";
            case 3: return "已完成";
            case 4: return "已取消";
            default: return "未知";
        }
    }

    @Override
    public List<PurchaseOrderDetailDTO> listOrders(String storeCode, Integer status) {
        QueryWrapper<PurchaseOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("store_code", storeCode);
        if (status != null) {
            wrapper.eq("order_status", status);
        }
        wrapper.orderByDesc("created_time");
        List<PurchaseOrder> orders = purchaseOrderMapper.selectList(wrapper);

        List<PurchaseOrderDetailDTO> result = new ArrayList<>();
        for (PurchaseOrder order : orders) {
            PurchaseOrderDetailDTO dto = new PurchaseOrderDetailDTO();
            dto.setOrderNo(order.getOrderNo());
            dto.setSupplierCode(order.getSupplierCode());
            dto.setSupplierName(order.getSupplierName());
            dto.setStoreCode(order.getStoreCode());
            dto.setStoreName(order.getStoreName());
            dto.setOrderStatus(order.getOrderStatus());
            dto.setOrderStatusText(getOrderStatusText(order.getOrderStatus()));
            dto.setTotalAmount(order.getTotalAmount());
            dto.setTotalQuantity(order.getTotalQuantity());
            dto.setOrderDate(order.getOrderDate());
            dto.setExpectedDate(order.getExpectedDate());
            dto.setBuyer(order.getBuyer());
            dto.setRemark(order.getRemark());
            dto.setCreatedTime(order.getCreatedTime());

            QueryWrapper<PurchaseOrderItem> itemWrapper = new QueryWrapper<>();
            itemWrapper.eq("order_no", order.getOrderNo());
            List<PurchaseOrderItem> items = purchaseOrderItemMapper.selectList(itemWrapper);
            List<PurchaseOrderDetailDTO.ItemDetail> itemDetails = items.stream().map(item -> {
                PurchaseOrderDetailDTO.ItemDetail id = new PurchaseOrderDetailDTO.ItemDetail();
                id.setId(item.getId());
                id.setSkuCode(item.getSkuCode());
                id.setProductName(item.getProductName());
                id.setCategoryCode(item.getCategoryCode());
                id.setQuantity(item.getQuantity());
                id.setPurchasePrice(item.getPurchasePrice());
                id.setAmount(item.getAmount());
                id.setReceivedQuantity(item.getReceivedQuantity());
                id.setRemark(item.getRemark());
                return id;
            }).collect(Collectors.toList());
            dto.setItems(itemDetails);

            result.add(dto);
        }
        return result;
    }
}
