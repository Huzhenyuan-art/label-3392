package com.example.lab3392.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lab3392.dto.ProductForm;
import com.example.lab3392.dto.ProductQuery;
import com.example.lab3392.entity.Product;
import com.example.lab3392.entity.ProductCategory;
import com.example.lab3392.mapper.ProductCategoryMapper;
import com.example.lab3392.mapper.ProductMapper;
import com.example.lab3392.service.ProductCategoryService;
import com.example.lab3392.service.ProductService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductMapper productMapper;
    private final ProductCategoryMapper categoryMapper;
    private final ProductCategoryService categoryService;

    public ProductServiceImpl(ProductMapper productMapper, ProductCategoryMapper categoryMapper, ProductCategoryService categoryService) {
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
        this.categoryService = categoryService;
    }

    private void populateCategoryNames(List<Product> products) {
        if (products == null || products.isEmpty()) return;
        List<Long> categoryIds = products.stream()
                .map(Product::getCategoryId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (categoryIds.isEmpty()) return;

        List<ProductCategory> categories = categoryMapper.selectBatchIds(categoryIds);
        Map<Long, String> categoryNameMap = new HashMap<>();
        for (ProductCategory c : categories) {
            categoryNameMap.put(c.getId(), c.getName());
        }
        for (Product p : products) {
            p.setCategoryName(categoryNameMap.get(p.getCategoryId()));
        }
    }

    @Override
    @Cacheable(cacheNames = "productPagesV4",
            key = "(#q.normalizedName()?:'') + '|' + (#q.minPrice()?:'') + '|' + (#q.maxPrice()?:'') + '|' + #page + '|' + #size")
    public IPage<Product> search(ProductQuery q, long page, long size) {
        String name = q.normalizedName();
        BigDecimal min = q.minPrice();
        BigDecimal max = q.maxPrice();

        LambdaQueryWrapper<Product> w = new LambdaQueryWrapper<>();
        if (name != null) w.like(Product::getName, name);
        if (min != null) w.ge(Product::getPrice, min);
        if (max != null) w.le(Product::getPrice, max);
        w.orderByDesc(Product::getUpdatedAt, Product::getId);

        IPage<Product> result = productMapper.selectPage(new Page<>(page, size), w);
        populateCategoryNames(result.getRecords());
        return result;
    }

    @Override
    public Product getByIdOrThrow(Long id) {
        Product p = productMapper.selectById(id);
        if (p == null) throw new IllegalArgumentException("产品不存在");
        return p;
    }

    @Override
    public Product getByIdWithCategory(Long id) {
        Product p = getByIdOrThrow(id);
        if (p.getCategoryId() != null) {
            ProductCategory c = categoryMapper.selectById(p.getCategoryId());
            if (c != null) {
                p.setCategoryName(c.getName());
            }
        }
        return p;
    }

    private void validateCategory(Long categoryId) {
        ProductCategory c = categoryService.getByIdOrThrow(categoryId);
        if (!"ACTIVE".equals(c.getStatus())) {
            throw new IllegalArgumentException("所选分类已停用，请选择有效分类");
        }
    }

    @Override
    @CacheEvict(cacheNames = {"productPagesV4", "productPagesV3", "productPagesV2", "productPages"}, allEntries = true)
    public Product create(ProductForm form) {
        validateCategory(form.categoryId());
        Product p = new Product();
        p.setCategoryId(form.categoryId());
        p.setName(form.name().trim());
        p.setDescription(form.description());
        p.setPrice(form.price());
        p.setStock(form.stock());
        p.setStatus(form.status());
        productMapper.insert(p);
        return p;
    }

    @Override
    @CacheEvict(cacheNames = {"productPagesV4", "productPagesV3", "productPagesV2", "productPages"}, allEntries = true)
    public void update(Long id, ProductForm form) {
        Product existing = getByIdOrThrow(id);
        validateCategory(form.categoryId());
        existing.setCategoryId(form.categoryId());
        existing.setName(form.name().trim());
        existing.setDescription(form.description());
        existing.setPrice(form.price());
        existing.setStock(form.stock());
        existing.setStatus(form.status());
        productMapper.updateById(existing);
    }

    @Override
    @CacheEvict(cacheNames = {"productPagesV4", "productPagesV3", "productPagesV2", "productPages"}, allEntries = true)
    public void delete(Long id) {
        Product existing = productMapper.selectById(id);
        if (existing == null) return;
        productMapper.deleteById(id);
    }
}
