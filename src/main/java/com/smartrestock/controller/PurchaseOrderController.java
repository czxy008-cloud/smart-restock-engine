package com.smartrestock.controller;

import com.smartrestock.dto.CommonResult;
import com.smartrestock.dto.PurchaseOrderDraftDTO;
import com.smartrestock.entity.PurchaseOrder;
import com.smartrestock.service.PurchaseOrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-order")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @GetMapping("/drafts")
    public CommonResult<List<PurchaseOrderDraftDTO>> generateDrafts(@RequestParam String storeCode) {
        return CommonResult.success(purchaseOrderService.generateDrafts(storeCode));
    }

    @PostMapping("/confirm")
    public CommonResult<PurchaseOrder> confirmOrder(@RequestParam String orderNo) {
        return CommonResult.success(purchaseOrderService.confirmOrder(orderNo));
    }

    @PostMapping("/create-from-draft")
    public CommonResult<PurchaseOrder> createFromDraft(@RequestBody PurchaseOrderDraftDTO draftDTO) {
        return CommonResult.success(purchaseOrderService.createPurchaseOrderFromDraft(draftDTO));
    }
}
