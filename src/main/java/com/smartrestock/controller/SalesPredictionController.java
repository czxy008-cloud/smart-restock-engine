package com.smartrestock.controller;

import com.smartrestock.dto.CommonResult;
import com.smartrestock.dto.SalesPredictionDTO;
import com.smartrestock.service.SalesPredictionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prediction")
public class SalesPredictionController {

    private final SalesPredictionService salesPredictionService;

    public SalesPredictionController(SalesPredictionService salesPredictionService) {
        this.salesPredictionService = salesPredictionService;
    }

    @GetMapping("/single")
    public CommonResult<SalesPredictionDTO> predict(@RequestParam String skuCode,
                                                     @RequestParam String storeCode) {
        return CommonResult.success(salesPredictionService.predict(skuCode, storeCode));
    }

    @GetMapping("/batch")
    public CommonResult<List<SalesPredictionDTO>> batchPredict(@RequestParam String storeCode,
                                                                @RequestParam(required = false) String categoryCode) {
        return CommonResult.success(salesPredictionService.batchPredict(storeCode, categoryCode));
    }
}
