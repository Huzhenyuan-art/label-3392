package com.example.lab3392.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lab3392.dto.CategoryForm;
import com.example.lab3392.dto.CategoryQuery;
import com.example.lab3392.entity.Product;
import com.example.lab3392.entity.ProductCategory;
import com.example.lab3392.mapper.ProductCategoryMapper;
import com.example.lab3392.mapper.ProductMapper;
import com.example.lab3392.service.ProductCategoryService;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ProductCategoryServiceImpl implements ProductCategoryService {
    private final ProductCategoryMapper categoryMapper;
    private final ProductMapper productMapper;

    public ProductCategoryServiceImpl(ProductCategoryMapper categoryMapper, ProductMapper productMapper) {
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
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
    @Cacheable(cacheNames = "activeCategoriesV1", key = "'all'")
    public List<ProductCategory> listAllActive() {
        LambdaQueryWrapper<ProductCategory> w = new LambdaQueryWrapper<>();
        w.eq(ProductCategory::getStatus, "ACTIVE");
        w.orderByAsc(ProductCategory::getSortOrder, ProductCategory::getId);
        return categoryMapper.selectList(w);
    }

    @Override
    public boolean hasProducts(Long categoryId) {
        if (categoryId == null) return false;
        LambdaQueryWrapper<Product> w = new LambdaQueryWrapper<>();
        w.eq(Product::getCategoryId, categoryId);
        return productMapper.selectCount(w) > 0;
    }

    @Override
    @CacheEvict(cacheNames = {"categoryPagesV1", "activeCategoriesV1"}, allEntries = true)
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
    @CacheEvict(cacheNames = {"categoryPagesV1", "activeCategoriesV1"}, allEntries = true)
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
    @CacheEvict(cacheNames = {"categoryPagesV1", "activeCategoriesV1"}, allEntries = true)
    public void delete(Long id) {
        ProductCategory existing = categoryMapper.selectById(id);
        if (existing == null) return;
        if (hasProducts(id)) {
            throw new IllegalStateException("该分类下存在产品，无法删除");
        }
        categoryMapper.deleteById(id);
    }

    @Override
    public ProductCategory findByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        LambdaQueryWrapper<ProductCategory> w = new LambdaQueryWrapper<>();
        w.eq(ProductCategory::getName, name.trim());
        return categoryMapper.selectOne(w);
    }
}
