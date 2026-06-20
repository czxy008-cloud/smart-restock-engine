package com.smartrestock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrestock.entity.SalesRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SalesRecordMapper extends BaseMapper<SalesRecord> {

    List<SalesRecord> selectRecentSales(@Param("skuCode") String skuCode, @Param("storeCode") String storeCode, @Param("days") Integer days);
}
