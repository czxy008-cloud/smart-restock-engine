package com.smartrestock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrestock.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
