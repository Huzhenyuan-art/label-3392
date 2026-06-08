package com.example.lab3392.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lab3392.dto.NotificationQuery;
import com.example.lab3392.entity.Notification;
import com.example.lab3392.entity.Product;
import com.example.lab3392.entity.Role;
import com.example.lab3392.entity.User;
import com.example.lab3392.entity.UserRole;
import com.example.lab3392.mapper.NotificationMapper;
import com.example.lab3392.mapper.ProductMapper;
import com.example.lab3392.mapper.RoleMapper;
import com.example.lab3392.mapper.UserMapper;
import com.example.lab3392.mapper.UserRoleMapper;
import com.example.lab3392.service.CacheService;
import com.example.lab3392.service.NotificationService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final ProductMapper productMapper;
    private final CacheManager cacheManager;
    private final CacheService cacheService;
    private static final String NOTIFICATION_PAGE_CACHE = "notificationPagesV1";
    private static final String NOTIFICATION_UNREAD_CACHE = "notificationUnreadV1";
    private static final String[] NOTIFICATION_CACHE_NAMES = {
            NOTIFICATION_PAGE_CACHE, NOTIFICATION_UNREAD_CACHE
    };

    public NotificationServiceImpl(NotificationMapper notificationMapper, UserMapper userMapper,
                                   UserRoleMapper userRoleMapper, RoleMapper roleMapper,
                                   ProductMapper productMapper, CacheManager cacheManager,
                                   CacheService cacheService) {
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.productMapper = productMapper;
        this.cacheManager = cacheManager;
        this.cacheService = cacheService;
    }

    private void populateProductNames(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) return;
        List<Long> productIds = notifications.stream()
                .map(Notification::getProductId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (productIds.isEmpty()) return;

        List<Product> products = productMapper.selectBatchIds(productIds);
        Map<Long, String> productNameMap = new HashMap<>();
        for (Product p : products) {
            productNameMap.put(p.getId(), p.getName());
        }
        for (Notification n : notifications) {
            n.setProductName(productNameMap.get(n.getProductId()));
        }
    }

    private List<Long> getRegularUserIds() {
        Role userRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, "USER"));
        if (userRole == null) return new ArrayList<>();

        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, userRole.getId())
        );
        return userRoles.stream()
                .map(UserRole::getUserId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(cacheNames = NOTIFICATION_PAGE_CACHE,
            key = "#userId + '|' + (#q.unreadOnly()?:'false') + '|' + (#q.type()?:'') + '|' + #page + '|' + #size")
    public IPage<Notification> search(Long userId, NotificationQuery q, long page, long size) {
        LambdaQueryWrapper<Notification> w = new LambdaQueryWrapper<>();
        w.eq(Notification::getUserId, userId);

        if (q.normalizedUnreadOnly()) {
            w.eq(Notification::getIsRead, 0);
        }
        if (q.type() != null && !q.type().trim().isEmpty()) {
            w.eq(Notification::getType, q.type().trim());
        }
        w.orderByDesc(Notification::getCreatedAt, Notification::getId);

        IPage<Notification> result = notificationMapper.selectPage(new Page<>(page, size), w);
        populateProductNames(result.getRecords());
        return result;
    }

    @Override
    @Cacheable(cacheNames = NOTIFICATION_UNREAD_CACHE, key = "#userId")
    public long countUnread(Long userId) {
        LambdaQueryWrapper<Notification> w = new LambdaQueryWrapper<>();
        w.eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0);
        return notificationMapper.selectCount(w);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {NOTIFICATION_PAGE_CACHE, NOTIFICATION_UNREAD_CACHE},
            allEntries = true, condition = "#userId != null")
    public Notification create(Long userId, String type, String title, String content, Long productId) {
        if (userId == null) throw new IllegalArgumentException("用户ID不能为空");
        if (type == null || type.trim().isEmpty()) throw new IllegalArgumentException("通知类型不能为空");
        if (title == null || title.trim().isEmpty()) throw new IllegalArgumentException("通知标题不能为空");

        User user = userMapper.selectById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");

        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type.trim());
        n.setTitle(title.trim());
        n.setContent(content != null ? content.trim() : null);
        n.setProductId(productId);
        n.setIsRead(0);
        notificationMapper.insert(n);
        return n;
    }

    @Override
    @Transactional
    public void createNotificationsForAllUsers(String type, String title, String content, Long productId) {
        List<Long> userIds = getRegularUserIds();
        for (Long userId : userIds) {
            create(userId, type, title, content, productId);
        }
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {NOTIFICATION_PAGE_CACHE, NOTIFICATION_UNREAD_CACHE},
            allEntries = true, condition = "#userId != null")
    public void markAsRead(Long userId, Long notificationId) {
        Notification n = notificationMapper.selectById(notificationId);
        if (n == null) throw new IllegalArgumentException("通知不存在");
        if (!n.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此通知");
        }
        if (n.getIsRead() == 1) return;

        n.setIsRead(1);
        n.setReadAt(LocalDateTime.now());
        notificationMapper.updateById(n);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {NOTIFICATION_PAGE_CACHE, NOTIFICATION_UNREAD_CACHE},
            allEntries = true, condition = "#userId != null")
    public void markAllAsRead(Long userId) {
        LambdaQueryWrapper<Notification> w = new LambdaQueryWrapper<>();
        w.eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0);

        Notification update = new Notification();
        update.setIsRead(1);
        update.setReadAt(LocalDateTime.now());
        notificationMapper.update(update, w);
    }

    @Override
    public void evictNotificationCaches(Long userId) {
        cacheService.evictAllNotificationCaches();
    }

    @Override
    public void evictAllNotificationCaches() {
        cacheService.evictAllNotificationCaches();
    }
}
