package com.example.lab3392.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.lab3392.dto.ChangePasswordForm;
import com.example.lab3392.dto.ProfileForm;
import com.example.lab3392.dto.UserQuery;
import com.example.lab3392.entity.User;

public interface UserService {
    void register(String username, String email, String rawPassword);

    User findByUsername(String username);

    User findById(Long id);

    User getByIdOrThrow(Long id);

    void updateEmail(String username, ProfileForm form);

    void changePassword(String username, ChangePasswordForm form);

    IPage<User> search(UserQuery q, long page, long size);

    void toggleEnabled(Long id, Long currentUserId);
}

