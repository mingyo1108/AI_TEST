package com.example.shop.product.mapper;

import com.example.shop.product.domain.Product;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 상품 매퍼. SQL은 {@code resources/mapper/product/ProductMapper.xml}.
 */
@Mapper
public interface ProductMapper {

    /** 판매 중인 상품을 조회한다. */
    Optional<Product> selectProductOne(@Param("productCode") String productCode);
}
