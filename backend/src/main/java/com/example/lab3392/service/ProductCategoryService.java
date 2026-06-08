package com.example.lab3392.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.lab3392.dto.CategoryForm;
import com.example.lab3392.dto.CategoryQuery;
import com.example.lab3392.entity.ProductCategory;
import java.util.List;

public interface ProductCategoryService {
    IPage<ProductCategory> search(CategoryQuery q, long page, long size);

    ProductCategory getByIdOrThrow(Long id);

    List<ProductCategory> listAllActive();

    boolean hasProducts(Long categoryId);

    void create(CategoryForm form);

    void update(Long id, CategoryForm form);

    void delete(Long id);

    ProductCategory findByName(String name);
}
