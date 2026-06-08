package com.example.lab3392.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.lab3392.dto.OperationLogQuery;
import com.example.lab3392.entity.Product;

public interface OperationLogService {
    void logProductCreate(Product product, Long operatorId, String operatorUsername);

    void logProductUpdate(Product before, Product after, Long operatorId, String operatorUsername);

    void logProductDelete(Product product, Long operatorId, String operatorUsername);

    IPage<?> search(OperationLogQuery q, long page, long size);
}
