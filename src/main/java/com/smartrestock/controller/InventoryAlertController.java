package com.smartrestock.controller;

import com.smartrestock.dto.CommonResult;
import com.smartrestock.dto.InventoryAlertDTO;
import com.smartrestock.service.InventoryAlertService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alert")
public class InventoryAlertController {

    private final InventoryAlertService inventoryAlertService;

    public InventoryAlertController(InventoryAlertService inventoryAlertService) {
        this.inventoryAlertService = inventoryAlertService;
    }

    @GetMapping("/list")
    public CommonResult<List<InventoryAlertDTO>> getAlertList(@RequestParam(required = false) String storeCode,
                                                               @RequestParam(required = false) String categoryCode) {
        return CommonResult.success(inventoryAlertService.getAlertList(storeCode, categoryCode));
    }

    @GetMapping("/detail")
    public CommonResult<InventoryAlertDTO> getAlertDetail(@RequestParam String skuCode,
                                                           @RequestParam String storeCode) {
        return CommonResult.success(inventoryAlertService.getAlertDetail(skuCode, storeCode));
    }

    @PostMapping("/refresh")
    public CommonResult<String> refreshAlertStatus() {
        inventoryAlertService.refreshAlertStatus();
        return CommonResult.success("刷新完成");
    }
}
