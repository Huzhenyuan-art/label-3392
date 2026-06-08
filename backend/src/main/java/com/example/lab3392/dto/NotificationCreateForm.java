package com.example.lab3392.dto;

public record NotificationCreateForm(
        Long userId,
        String type,
        String title,
        String content,
        Long productId
) {}
