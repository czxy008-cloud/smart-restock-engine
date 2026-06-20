package com.smartrestock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrestock.entity.PurchaseOrderItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PurchaseOrderItemMapper extends BaseMapper<PurchaseOrderItem> {
}
