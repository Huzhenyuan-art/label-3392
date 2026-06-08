package com.example.lab3392.dto;

public record NotificationQuery(
        Boolean unreadOnly,
        String type
) {
    public Boolean normalizedUnreadOnly() {
        return unreadOnly != null && unreadOnly;
    }
}
