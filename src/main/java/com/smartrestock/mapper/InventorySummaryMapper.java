package com.smartrestock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrestock.entity.InventorySummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InventorySummaryMapper extends BaseMapper<InventorySummary> {

    List<InventorySummary> selectAlertList(@Param("storeCode") String storeCode, @Param("categoryCode") String categoryCode);
}
