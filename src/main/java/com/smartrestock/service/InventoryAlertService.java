package com.smartrestock.service;

import com.smartrestock.dto.InventoryAlertDTO;

import java.util.List;

public interface InventoryAlertService {

    List<InventoryAlertDTO> getAlertList(String storeCode, String categoryCode);

    InventoryAlertDTO getAlertDetail(String skuCode, String storeCode);

    void refreshAlertStatus();
}
