package com.example.shop.product.application;

import com.example.shop.common.BusinessException;
import com.example.shop.common.ErrorCode;
import com.example.shop.product.domain.Product;
import com.example.shop.product.mapper.ProductMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 서비스. 다른 기능은 상품 정보를 이 서비스로 가져온다.
 */
@Service
public class ProductService {

    private final ProductMapper productMapper;

    public ProductService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    /** 상품을 조회한다. 없으면 404. */
    @Transactional(readOnly = true)
    public Product getProduct(String productCode) {
        return productMapper.selectProductOne(productCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, productCode));
    }
}
