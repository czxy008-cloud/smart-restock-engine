package com.smartrestock.service;

import com.smartrestock.dto.PurchaseOrderDraftDTO;
import com.smartrestock.entity.PurchaseOrder;

import java.util.List;

public interface PurchaseOrderService {

    List<PurchaseOrderDraftDTO> generateDrafts(String storeCode);

    PurchaseOrder confirmOrder(String orderNo);
}
