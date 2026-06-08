package com.example.lab3392.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.lab3392.dto.CategoryForm;
import com.example.lab3392.dto.CategoryQuery;
import com.example.lab3392.entity.Product;
import com.example.lab3392.entity.ProductCategory;
import com.example.lab3392.mapper.ProductCategoryMapper;
import com.example.lab3392.mapper.ProductMapper;
import com.example.lab3392.testsupport.DbTestSupport;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ProductCategoryServiceImplTest extends DbTestSupport {
    @Autowired
    ProductCategoryService categoryService;

    @Autowired
    ProductCategoryMapper categoryMapper;

    @Autowired
    ProductMapper productMapper;

    @Test
    void search_filtersByNameAndCode_andPaginates() {
        insertCategory("笔记本电脑", "LAPTOP", 1);
        insertCategory("外设配件", "ACCESSORY", 2);
        insertCategory("显示器", "MONITOR", 3);

        IPage<ProductCategory> page = categoryService.search(new CategoryQuery("笔记", "LAP"), 1, 10);
        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getRecords()).extracting(ProductCategory::getName).containsExactly("笔记本电脑");
    }

    @Test
    void create_update_delete_work() {
        CategoryForm form = new CategoryForm(null, "智能穿戴", "WEARABLE", "智能手表手环", 5, "ACTIVE");
        categoryService.create(form);

        ProductCategory created = categoryMapper.selectOne(new LambdaQueryWrapper<ProductCategory>().eq(ProductCategory::getCode, "WEARABLE"));
        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("智能穿戴");
        assertThat(created.getSortOrder()).isEqualTo(5);

        categoryService.update(created.getId(), new CategoryForm(created.getId(), "智能穿戴设备", "WEARABLE", "新描述", 6, "INACTIVE"));
        ProductCategory updated = categoryMapper.selectById(created.getId());
        assertThat(updated.getName()).isEqualTo("智能穿戴设备");
        assertThat(updated.getSortOrder()).isEqualTo(6);
        assertThat(updated.getStatus()).isEqualTo("INACTIVE");

        categoryService.delete(created.getId());
        assertThat(categoryMapper.selectById(created.getId())).isNull();
    }

    @Test
    void delete_rejectsWhenHasProducts() {
        ProductCategory category = insertCategory("测试分类", "TEST", 1);

        Product p = new Product();
        p.setCategoryId(category.getId());
        p.setName("测试产品");
        p.setPrice(new BigDecimal("99.00"));
        p.setStock(10);
        p.setStatus("ACTIVE");
        productMapper.insert(p);

        assertThat(categoryService.hasProducts(category.getId())).isTrue();
        assertThatThrownBy(() -> categoryService.delete(category.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("存在产品");

        assertThat(categoryMapper.selectById(category.getId())).isNotNull();
    }

    @Test
    void listAllActive_returnsOnlyActiveCategories() {
        insertCategory("Active1", "ACT1", 1);
        insertCategory("Inactive", "INACT", 2, "INACTIVE");
        insertCategory("Active2", "ACT2", 3);

        List<ProductCategory> active = categoryService.listAllActive();
        assertThat(active).extracting(ProductCategory::getCode).containsExactly("ACT1", "ACT2");
    }

    @Test
    void getByIdOrThrow_throwsWhenMissing() {
        assertThatThrownBy(() -> categoryService.getByIdOrThrow(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("分类");
    }

    @Test
    void search_sortsBySortOrderAsc() {
        insertCategory("B", "B", 3);
        insertCategory("A", "A", 1);
        insertCategory("C", "C", 2);

        IPage<ProductCategory> page = categoryService.search(new CategoryQuery(null, null), 1, 10);
        assertThat(page.getRecords()).extracting(ProductCategory::getCode).containsExactly("A", "C", "B");
    }

    @Test
    void search_paginatesWithSize10() {
        for (int i = 1; i <= 25; i++) {
            insertCategory("Cat " + i, "CAT" + i, i);
        }

        IPage<ProductCategory> p1 = categoryService.search(new CategoryQuery(null, null), 1, 10);
        IPage<ProductCategory> p2 = categoryService.search(new CategoryQuery(null, null), 2, 10);
        IPage<ProductCategory> p3 = categoryService.search(new CategoryQuery(null, null), 3, 10);

        assertThat(p1.getTotal()).isEqualTo(25);
        assertThat(p1.getPages()).isEqualTo(3);
        assertThat(p1.getRecords()).hasSize(10);
        assertThat(p2.getRecords()).hasSize(10);
        assertThat(p3.getRecords()).hasSize(5);
    }

    private ProductCategory insertCategory(String name, String code, int sortOrder) {
        return insertCategory(name, code, sortOrder, "ACTIVE");
    }

    private ProductCategory insertCategory(String name, String code, int sortOrder, String status) {
        ProductCategory c = new ProductCategory();
        c.setName(name);
        c.setCode(code);
        c.setSortOrder(sortOrder);
        c.setStatus(status);
        categoryMapper.insert(c);
        return c;
    }
}
