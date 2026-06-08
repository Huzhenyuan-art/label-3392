package com.example.lab3392.dto;

public record UserQuery(
        String username,
        String email,
        Integer enabled
) {
    public static UserQuery empty() {
        return new UserQuery("", "", null);
    }
}
