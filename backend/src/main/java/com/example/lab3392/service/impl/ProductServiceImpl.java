package com.example.lab3392.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lab3392.dto.CsvImportResult;
import com.example.lab3392.dto.ProductForm;
import com.example.lab3392.dto.ProductQuery;
import com.example.lab3392.entity.Product;
import com.example.lab3392.entity.ProductCategory;
import com.example.lab3392.mapper.ProductCategoryMapper;
import com.example.lab3392.mapper.ProductMapper;
import com.example.lab3392.service.CacheService;
import com.example.lab3392.service.ProductCategoryService;
import com.example.lab3392.service.ProductService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductMapper productMapper;
    private final ProductCategoryMapper categoryMapper;
    private final ProductCategoryService categoryService;
    private final CacheService cacheService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String[] CSV_HEADERS = {"名称", "分类", "描述", "价格", "库存", "状态", "创建时间", "更新时间"};

    public ProductServiceImpl(ProductMapper productMapper, ProductCategoryMapper categoryMapper,
                              ProductCategoryService categoryService, CacheService cacheService) {
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
        this.categoryService = categoryService;
        this.cacheService = cacheService;
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

    @Override
    public List<Product> findAllByQuery(ProductQuery q) {
        String name = q.normalizedName();
        BigDecimal min = q.minPrice();
        BigDecimal max = q.maxPrice();

        LambdaQueryWrapper<Product> w = new LambdaQueryWrapper<>();
        if (name != null) w.like(Product::getName, name);
        if (min != null) w.ge(Product::getPrice, min);
        if (max != null) w.le(Product::getPrice, max);
        w.orderByDesc(Product::getUpdatedAt, Product::getId);

        List<Product> products = productMapper.selectList(w);
        populateCategoryNames(products);
        return products;
    }

    @Override
    public void exportCsv(ProductQuery q, OutputStream outputStream) {
        List<Product> products = findAllByQuery(q);
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.EXCEL.builder()
                     .setHeader(CSV_HEADERS)
                     .build())) {

            writer.print('\uFEFF');
            writer.flush();

            for (Product p : products) {
                List<String> row = new ArrayList<>();
                row.add(p.getName() != null ? p.getName() : "");
                row.add(p.getCategoryName() != null ? p.getCategoryName() : "");
                row.add(p.getDescription() != null ? p.getDescription() : "");
                row.add(p.getPrice() != null ? p.getPrice().toString() : "0");
                row.add(p.getStock() != null ? p.getStock().toString() : "0");
                row.add(p.getStatus() != null ? p.getStatus() : "");
                row.add(p.getCreatedAt() != null ? p.getCreatedAt().format(DATE_FORMATTER) : "");
                row.add(p.getUpdatedAt() != null ? p.getUpdatedAt().format(DATE_FORMATTER) : "");
                csvPrinter.printRecord(row);
            }
            csvPrinter.flush();
        } catch (IOException e) {
            throw new RuntimeException("导出CSV失败", e);
        }
    }

    @Override
    @Transactional
    public CsvImportResult importCsv(InputStream inputStream) {
        CsvImportResult result = new CsvImportResult();
        List<Product> productsToSave = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser csvParser = CSVFormat.EXCEL.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            Map<String, ProductCategory> categoryCache = new HashMap<>();
            Map<String, Product> existingProductCache = new HashMap<>();

            for (CSVRecord record : csvParser) {
                int rowNum = (int) record.getRecordNumber();
                result.setTotalRows(result.getTotalRows() + 1);

                String name = getValueOrNull(record, "名称", "name");
                String categoryName = getValueOrNull(record, "分类", "category", "categoryName");
                String description = getValueOrNull(record, "描述", "description");
                String priceStr = getValueOrNull(record, "价格", "price");
                String stockStr = getValueOrNull(record, "库存", "stock");
                String status = getValueOrNull(record, "状态", "status");

                try {
                    validateRow(name, categoryName, priceStr, stockStr, status);

                    ProductCategory category = categoryCache.get(categoryName);
                    if (category == null) {
                        category = categoryService.findByName(categoryName);
                        if (category == null) {
                            throw new IllegalArgumentException("分类不存在: " + categoryName);
                        }
                        if (!"ACTIVE".equals(category.getStatus())) {
                            throw new IllegalArgumentException("分类已停用: " + categoryName);
                        }
                        categoryCache.put(categoryName, category);
                    }

                    BigDecimal price = new BigDecimal(priceStr.trim());
                    if (price.compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalArgumentException("价格不能小于0");
                    }

                    int stock;
                    try {
                        stock = Integer.parseInt(stockStr.trim());
                        if (stock < 0) {
                            throw new IllegalArgumentException("库存不能小于0");
                        }
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("库存必须是有效整数");
                    }

                    String normalizedName = name.trim();
                    if (normalizedName.length() > 50) {
                        throw new IllegalArgumentException("名称长度不能超过50");
                    }
                    if (description != null && description.length() > 255) {
                        throw new IllegalArgumentException("描述长度不能超过255");
                    }

                    String statusValue = (status != null) ? status.trim().toUpperCase() : "ACTIVE";
                    if (!"ACTIVE".equals(statusValue) && !"INACTIVE".equals(statusValue)) {
                        throw new IllegalArgumentException("状态必须是ACTIVE或INACTIVE");
                    }

                    Product existing = existingProductCache.get(normalizedName);
                    if (existing == null) {
                        existing = findByName(normalizedName);
                        if (existing != null) {
                            existingProductCache.put(normalizedName, existing);
                        }
                    }

                    Product product;
                    if (existing != null) {
                        product = existing;
                        product.setCategoryId(category.getId());
                        product.setName(normalizedName);
                        product.setDescription(description);
                        product.setPrice(price);
                        product.setStock(stock);
                        product.setStatus(statusValue);
                    } else {
                        product = new Product();
                        product.setCategoryId(category.getId());
                        product.setName(normalizedName);
                        product.setDescription(description);
                        product.setPrice(price);
                        product.setStock(stock);
                        product.setStatus(statusValue);
                    }

                    productsToSave.add(product);
                    result.addSuccess();

                } catch (Exception e) {
                    result.addFailure(rowNum, name != null ? name : "", e.getMessage());
                }
            }

            for (Product product : productsToSave) {
                if (product.getId() != null) {
                    productMapper.updateById(product);
                } else {
                    productMapper.insert(product);
                }
            }

            if (!productsToSave.isEmpty()) {
                cacheService.evictAllProductCaches();
            }

        } catch (IOException e) {
            throw new RuntimeException("读取CSV文件失败", e);
        }

        return result;
    }

    private String getValueOrNull(CSVRecord record, String... keys) {
        for (String key : keys) {
            if (record.isMapped(key)) {
                String value = record.get(key);
                if (value != null && !value.trim().isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    private void validateRow(String name, String categoryName, String priceStr, String stockStr, String status) {
        List<String> missingFields = new ArrayList<>();
        if (name == null || name.trim().isEmpty()) {
            missingFields.add("名称");
        }
        if (categoryName == null || categoryName.trim().isEmpty()) {
            missingFields.add("分类");
        }
        if (priceStr == null || priceStr.trim().isEmpty()) {
            missingFields.add("价格");
        }
        if (stockStr == null || stockStr.trim().isEmpty()) {
            missingFields.add("库存");
        }
        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException("缺少必填字段: " + String.join(", ", missingFields));
        }
    }

    private Product findByName(String name) {
        LambdaQueryWrapper<Product> w = new LambdaQueryWrapper<>();
        w.eq(Product::getName, name);
        return productMapper.selectOne(w);
    }
}
