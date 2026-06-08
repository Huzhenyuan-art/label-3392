package com.example.lab3392.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lab3392.dto.ProductForm;
import com.example.lab3392.dto.ProductQuery;
import com.example.lab3392.entity.Product;
import com.example.lab3392.mapper.ProductMapper;
import com.example.lab3392.service.ProductService;
import java.math.BigDecimal;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductMapper productMapper;

    public ProductServiceImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    @Cacheable(cacheNames = "productPagesV3",
            key = "(#q.normalizedName()?:'') + '|' + (#q.minPrice()?:'') + '|' + (#q.maxPrice()?:'') + '|' + #page + '|' + #size")
    public IPage<Product> search(ProductQuery q, long page, long size) {
        String name = q.normalizedName();
        BigDecimal min = q.minPrice();
        BigDecimal max = q.maxPrice();

        LambdaQueryWrapper<Product> w = new LambdaQueryWrapper<>();
        if (name != null) w.like(Product::getName, name);
        if (min != null) w.ge(Product::getPrice, min);
        if (max != null) w.le(Product::getPrice, max);
        // Ensure a deterministic order for offset pagination:
        // many rows can share the same updatedAt (e.g. seed/import within the same second),
        // so we add id as a tiebreaker to avoid duplicates across pages.
        w.orderByDesc(Product::getUpdatedAt, Product::getId);

        return productMapper.selectPage(new Page<>(page, size), w);
    }

    @Override
    public Product getByIdOrThrow(Long id) {
        Product p = productMapper.selectById(id);
        if (p == null) throw new IllegalArgumentException("产品不存在");
        return p;
    }

    @Override
    @CacheEvict(cacheNames = {"productPagesV3", "productPagesV2", "productPages"}, allEntries = true)
    public void create(ProductForm form) {
        Product p = new Product();
        p.setName(form.name().trim());
        p.setDescription(form.description());
        p.setPrice(form.price());
        p.setStock(form.stock());
        p.setStatus(form.status());
        productMapper.insert(p);
    }

    @Override
    @CacheEvict(cacheNames = {"productPagesV3", "productPagesV2", "productPages"}, allEntries = true)
    public void update(Long id, ProductForm form) {
        Product existing = getByIdOrThrow(id);
        existing.setName(form.name().trim());
        existing.setDescription(form.description());
        existing.setPrice(form.price());
        existing.setStock(form.stock());
        existing.setStatus(form.status());
        productMapper.updateById(existing);
    }

    @Override
    @CacheEvict(cacheNames = {"productPagesV3", "productPagesV2", "productPages"}, allEntries = true)
    public void delete(Long id) {
        Product existing = productMapper.selectById(id);
        if (existing == null) return;
        productMapper.deleteById(id);
    }
}
