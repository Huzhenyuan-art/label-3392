package com.example.lab3392.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.lab3392.dto.ProductForm;
import com.example.lab3392.dto.ProductQuery;
import com.example.lab3392.entity.Product;
import com.example.lab3392.entity.ProductCategory;
import com.example.lab3392.mapper.ProductCategoryMapper;
import com.example.lab3392.mapper.ProductMapper;
import com.example.lab3392.testsupport.DbTestSupport;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ProductServiceImplTest extends DbTestSupport {
    @Autowired
    ProductService productService;

    @Autowired
    ProductMapper productMapper;

    @Autowired
    ProductCategoryMapper categoryMapper;

    Long testCategoryId;

    @BeforeEach
    void setupTestCategory() {
        ProductCategory c = new ProductCategory();
        c.setName("测试分类");
        c.setCode("TEST_CAT");
        c.setSortOrder(1);
        c.setStatus("ACTIVE");
        categoryMapper.insert(c);
        testCategoryId = c.getId();
    }

    @Test
    void search_filtersByNameAndPriceRange_andPaginates() {
        insertProduct("Aurora Pro", new BigDecimal("100.00"));
        insertProduct("Aurora Lite", new BigDecimal("10.00"));
        insertProduct("Neon Dock", new BigDecimal("50.00"));

        IPage<Product> page = productService.search(new ProductQuery("Aurora", new BigDecimal("20.00"), new BigDecimal("200.00")), 1, 10);
        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getRecords()).extracting(Product::getName).containsExactly("Aurora Pro");
        assertThat(page.getRecords().get(0).getCategoryName()).isNotNull();
    }

    @Test
    void create_update_delete_work() {
        ProductForm form = new ProductForm(null, testCategoryId, "Quantum Mouse", "desc", new BigDecimal("259.00"), 80, "ACTIVE");
        productService.create(form, null);

        Product created = productMapper.selectOne(new LambdaQueryWrapper<Product>().eq(Product::getName, "Quantum Mouse"));
        assertThat(created).isNotNull();
        assertThat(created.getPrice()).isEqualByComparingTo("259.00");
        assertThat(created.getCategoryId()).isEqualTo(testCategoryId);

        productService.update(created.getId(), new ProductForm(created.getId(), testCategoryId, "Quantum Mouse X", "d2", new BigDecimal("299.00"), 81, "INACTIVE"), null);
        Product updated = productMapper.selectById(created.getId());
        assertThat(updated.getName()).isEqualTo("Quantum Mouse X");
        assertThat(updated.getStatus()).isEqualTo("INACTIVE");

        productService.delete(created.getId());
        assertThat(productMapper.selectById(created.getId())).isNull();
    }

    @Test
    void create_rejectsInactiveCategory() {
        ProductCategory inactive = new ProductCategory();
        inactive.setName("停用分类");
        inactive.setCode("INACTIVE_CAT");
        inactive.setSortOrder(2);
        inactive.setStatus("INACTIVE");
        categoryMapper.insert(inactive);

        ProductForm form = new ProductForm(null, inactive.getId(), "Test Product", "desc", new BigDecimal("100.00"), 10, "ACTIVE");
        assertThatThrownBy(() -> productService.create(form, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("分类已停用");
    }

    @Test
    void getByIdOrThrow_throwsWhenMissing() {
        assertThatThrownBy(() -> productService.getByIdOrThrow(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("产品");
    }

    @Test
    void getByIdWithCategory_returnsCategoryName() {
        Long productId = insertProduct("Test Product", new BigDecimal("99.00"));
        Product p = productService.getByIdWithCategory(productId);
        assertThat(p.getCategoryName()).isEqualTo("测试分类");
    }

    @Test
    void search_paginatesWithSize10() {
        for (int i = 1; i <= 21; i++) {
            insertProduct("Item " + i, new BigDecimal("1.00"));
        }

        IPage<Product> p1 = productService.search(new ProductQuery(null, null, null), 1, 10);
        IPage<Product> p2 = productService.search(new ProductQuery(null, null, null), 2, 10);
        IPage<Product> p3 = productService.search(new ProductQuery(null, null, null), 3, 10);

        assertThat(p1.getTotal()).isEqualTo(21);
        assertThat(p1.getPages()).isEqualTo(3);
        assertThat(p1.getRecords()).hasSize(10);
        assertThat(p2.getRecords()).hasSize(10);
        assertThat(p3.getRecords()).hasSize(1);
    }

    @Test
    void search_nameOnly_fuzzyMatches() {
        insertProduct("MacBook Pro 16", new BigDecimal("19999.00"));
        insertProduct("MacBook Air", new BigDecimal("8999.00"));
        insertProduct("ThinkPad X1", new BigDecimal("9999.00"));

        IPage<Product> page = productService.search(new ProductQuery("MacBook", null, null), 1, 10);
        assertThat(page.getTotal()).isEqualTo(2);
        assertThat(page.getRecords()).extracting(Product::getName)
                .containsExactlyInAnyOrder("MacBook Pro 16", "MacBook Air");
    }

    @Test
    void search_priceOnlyRange_filtersCorrectly() {
        insertProduct("Cheap", new BigDecimal("50.00"));
        insertProduct("Mid", new BigDecimal("150.00"));
        insertProduct("Expensive", new BigDecimal("500.00"));

        IPage<Product> page = productService.search(new ProductQuery(null, new BigDecimal("100.00"), new BigDecimal("300.00")), 1, 10);
        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getRecords()).extracting(Product::getName).containsExactly("Mid");
    }

    @Test
    void search_nameAndPriceAndPagination_combined() {
        for (int i = 1; i <= 15; i++) {
            insertProduct("Pro X" + i, new BigDecimal(String.valueOf(i * 10)));
        }
        insertProduct("Basic Z1", new BigDecimal("5.00"));

        IPage<Product> page1 = productService.search(new ProductQuery("Pro", new BigDecimal("50.00"), new BigDecimal("100.00")), 1, 5);
        assertThat(page1.getTotal()).isEqualTo(6);
        assertThat(page1.getRecords()).hasSize(5);
        assertThat(page1.getRecords()).allSatisfy(p -> {
            assertThat(p.getName()).startsWith("Pro");
            assertThat(p.getPrice()).isBetween(new BigDecimal("50.00"), new BigDecimal("100.00"));
        });

        IPage<Product> page2 = productService.search(new ProductQuery("Pro", new BigDecimal("50.00"), new BigDecimal("100.00")), 2, 5);
        assertThat(page2.getRecords()).hasSize(1);
    }

    @Test
    void search_priceRange_minEqualsMax_returnsExactMatch() {
        insertProduct("Exact", new BigDecimal("100.00"));
        insertProduct("Other", new BigDecimal("200.00"));

        IPage<Product> page = productService.search(new ProductQuery(null, new BigDecimal("100.00"), new BigDecimal("100.00")), 1, 10);
        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getRecords()).extracting(Product::getName).containsExactly("Exact");
    }

    @Test
    void getByIdOrThrow_existingId_returnsProduct() {
        Long id = insertProduct("Exists", new BigDecimal("42.00"));
        Product found = productService.getByIdOrThrow(id);
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Exists");
        assertThat(found.getPrice()).isEqualByComparingTo("42.00");
    }

    private Long insertProduct(String name, BigDecimal price) {
        Product p = new Product();
        p.setCategoryId(testCategoryId);
        p.setName(name);
        p.setPrice(price);
        p.setStock(1);
        p.setStatus("ACTIVE");
        productMapper.insert(p);
        return p.getId();
    }
}
