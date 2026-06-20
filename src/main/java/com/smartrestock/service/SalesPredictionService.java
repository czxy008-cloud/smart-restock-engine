package com.smartrestock.service;

import com.smartrestock.dto.SalesPredictionDTO;
import java.util.List;

public interface SalesPredictionService {

    SalesPredictionDTO predict(String skuCode, String storeCode);

    List<SalesPredictionDTO> batchPredict(String storeCode, String categoryCode);
}
