package com.example.lab3392.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.lab3392.dto.CsvImportResult;
import com.example.lab3392.dto.ProductForm;
import com.example.lab3392.dto.ProductQuery;
import com.example.lab3392.entity.Product;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ProductService {
    IPage<Product> search(ProductQuery q, long page, long size);

    Product getByIdOrThrow(Long id);

    Product getByIdWithCategory(Long id);

    Product create(ProductForm form, MultipartFile coverImage);

    void update(Long id, ProductForm form, MultipartFile coverImage);

    void delete(Long id);

    List<Product> findAllByQuery(ProductQuery q);

    void exportCsv(ProductQuery q, OutputStream outputStream);

    CsvImportResult importCsv(InputStream inputStream);
}

