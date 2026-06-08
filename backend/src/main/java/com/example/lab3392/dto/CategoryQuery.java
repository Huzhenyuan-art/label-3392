package com.example.lab3392.dto;

public record CategoryQuery(String name, String code) {
    public String normalizedName() {
        if (name == null) return null;
        String n = name.trim();
        return n.isEmpty() ? null : n;
    }

    public String normalizedCode() {
        if (code == null) return null;
        String c = code.trim();
        return c.isEmpty() ? null : c;
    }
}
