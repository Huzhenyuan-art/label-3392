package com.example.lab3392.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lab3392.dto.OperationLogQuery;
import com.example.lab3392.entity.OperationLog;
import com.example.lab3392.entity.Product;
import com.example.lab3392.mapper.OperationLogMapper;
import com.example.lab3392.service.OperationLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OperationLogServiceImpl implements OperationLogService {
    private static final Logger log = LoggerFactory.getLogger(OperationLogServiceImpl.class);
    private static final String TARGET_TYPE_PRODUCT = "PRODUCT";
    private static final String OP_CREATE = "CREATE";
    private static final String OP_UPDATE = "UPDATE";
    private static final String OP_DELETE = "DELETE";

    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    public OperationLogServiceImpl(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void logProductCreate(Product product, Long operatorId, String operatorUsername) {
        OperationLog oplog = new OperationLog();
        oplog.setOperatorId(operatorId);
        oplog.setOperatorUsername(operatorUsername);
        oplog.setOperationType(OP_CREATE);
        oplog.setTargetType(TARGET_TYPE_PRODUCT);
        oplog.setTargetId(product.getId());
        oplog.setBeforeSnapshot(null);
        oplog.setAfterSnapshot(toJson(product));
        oplog.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(oplog);
    }

    @Override
    public void logProductUpdate(Product before, Product after, Long operatorId, String operatorUsername) {
        OperationLog oplog = new OperationLog();
        oplog.setOperatorId(operatorId);
        oplog.setOperatorUsername(operatorUsername);
        oplog.setOperationType(OP_UPDATE);
        oplog.setTargetType(TARGET_TYPE_PRODUCT);
        oplog.setTargetId(after.getId());
        oplog.setBeforeSnapshot(toJson(before));
        oplog.setAfterSnapshot(toJson(after));
        oplog.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(oplog);
    }

    @Override
    public void logProductDelete(Product product, Long operatorId, String operatorUsername) {
        OperationLog oplog = new OperationLog();
        oplog.setOperatorId(operatorId);
        oplog.setOperatorUsername(operatorUsername);
        oplog.setOperationType(OP_DELETE);
        oplog.setTargetType(TARGET_TYPE_PRODUCT);
        oplog.setTargetId(product.getId());
        oplog.setBeforeSnapshot(toJson(product));
        oplog.setAfterSnapshot(null);
        oplog.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(oplog);
    }

    @Override
    public IPage<OperationLog> search(OperationLogQuery q, long page, long size) {
        String opType = q.normalizedOperationType();
        LocalDate start = q.normalizedStartDate();
        LocalDate end = q.normalizedEndDate();

        LambdaQueryWrapper<OperationLog> w = new LambdaQueryWrapper<>();
        if (opType != null) {
            w.eq(OperationLog::getOperationType, opType);
        }
        if (start != null) {
            w.ge(OperationLog::getCreatedAt, start.atStartOfDay());
        }
        if (end != null) {
            w.le(OperationLog::getCreatedAt, end.atTime(23, 59, 59));
        }
        w.orderByDesc(OperationLog::getCreatedAt, OperationLog::getId);

        return operationLogMapper.selectPage(new Page<>(page, size), w);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON", e);
            return null;
        }
    }
}
