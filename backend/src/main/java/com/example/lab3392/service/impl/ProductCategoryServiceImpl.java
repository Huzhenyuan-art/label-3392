package com.example.lab3392.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lab3392.dto.CategoryForm;
import com.example.lab3392.dto.CategoryQuery;
import com.example.lab3392.entity.ProductCategory;
import com.example.lab3392.mapper.ProductCategoryMapper;
import com.example.lab3392.service.ProductCategoryService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ProductCategoryServiceImpl implements ProductCategoryService {
    private final ProductCategoryMapper categoryMapper;

    public ProductCategoryServiceImpl(ProductCategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Override
    @Cacheable(cacheNames = "categoryPagesV1",
            key = "(#q.normalizedName()?:'') + '|' + (#q.normalizedCode()?:'') + '|' + #page + '|' + #size")
    public IPage<ProductCategory> search(CategoryQuery q, long page, long size) {
        String name = q.normalizedName();
        String code = q.normalizedCode();

        LambdaQueryWrapper<ProductCategory> w = new LambdaQueryWrapper<>();
        if (name != null) w.like(ProductCategory::getName, name);
        if (code != null) w.like(ProductCategory::getCode, code);
        w.orderByAsc(ProductCategory::getSortOrder, ProductCategory::getId);

        return categoryMapper.selectPage(new Page<>(page, size), w);
    }

    @Override
    public ProductCategory getByIdOrThrow(Long id) {
        ProductCategory c = categoryMapper.selectById(id);
        if (c == null) throw new IllegalArgumentException("分类不存在");
        return c;
    }

    @Override
    @CacheEvict(cacheNames = "categoryPagesV1", allEntries = true)
    public void create(CategoryForm form) {
        ProductCategory c = new ProductCategory();
        c.setName(form.name().trim());
        c.setCode(form.code().trim());
        c.setDescription(form.description());
        c.setSortOrder(form.sortOrder());
        c.setStatus(form.status());
        categoryMapper.insert(c);
    }

    @Override
    @CacheEvict(cacheNames = "categoryPagesV1", allEntries = true)
    public void update(Long id, CategoryForm form) {
        ProductCategory existing = getByIdOrThrow(id);
        existing.setName(form.name().trim());
        existing.setCode(form.code().trim());
        existing.setDescription(form.description());
        existing.setSortOrder(form.sortOrder());
        existing.setStatus(form.status());
        categoryMapper.updateById(existing);
    }

    @Override
    @CacheEvict(cacheNames = "categoryPagesV1", allEntries = true)
    public void delete(Long id) {
        ProductCategory existing = categoryMapper.selectById(id);
        if (existing == null) return;
        categoryMapper.deleteById(id);
    }
}
