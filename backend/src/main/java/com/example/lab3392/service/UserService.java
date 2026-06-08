package com.example.lab3392.service;

import com.example.lab3392.entity.User;

public interface UserService {
    void register(String username, String email, String rawPassword);

    User findByUsername(String username);
}

