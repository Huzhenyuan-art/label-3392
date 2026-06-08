package com.example.lab3392.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.lab3392.dto.NotificationQuery;
import com.example.lab3392.entity.Notification;

public interface NotificationService {
    IPage<Notification> search(Long userId, NotificationQuery q, long page, long size);

    long countUnread(Long userId);

    Notification create(Long userId, String type, String title, String content, Long productId);

    void createNotificationsForAllUsers(String type, String title, String content, Long productId);

    void markAsRead(Long userId, Long notificationId);

    void markAllAsRead(Long userId);

    void evictNotificationCaches(Long userId);

    void evictAllNotificationCaches();
}
