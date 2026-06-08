package com.example.lab3392.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lab3392.dto.ChangePasswordForm;
import com.example.lab3392.dto.ProfileForm;
import com.example.lab3392.dto.UserQuery;
import com.example.lab3392.entity.Role;
import com.example.lab3392.entity.User;
import com.example.lab3392.entity.UserRole;
import com.example.lab3392.mapper.RoleMapper;
import com.example.lab3392.mapper.UserMapper;
import com.example.lab3392.mapper.UserRoleMapper;
import com.example.lab3392.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper, RoleMapper roleMapper, UserRoleMapper userRoleMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void register(String username, String email, String rawPassword) {
        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email)) != null) {
            throw new IllegalArgumentException("邮箱已被使用");
        }

        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setEnabled(1);
        userMapper.insert(u);

        Role userRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, "USER"));
        if (userRole != null) {
            UserRole ur = new UserRole();
            ur.setUserId(u.getId());
            ur.setRoleId(userRole.getId());
            userRoleMapper.insert(ur);
        }
    }

    @Override
    public User findByUsername(String username) {
        if (username == null) return null;
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public User findById(Long id) {
        if (id == null) return null;
        return userMapper.selectById(id);
    }

    @Override
    public User getByIdOrThrow(Long id) {
        User u = findById(id);
        if (u == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return u;
    }

    @Override
    public void updateEmail(String username, ProfileForm form) {
        User u = findByUsername(username);
        if (u == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        String newEmail = form.email().trim();
        if (!u.getEmail().equals(newEmail)) {
            User existing = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, newEmail));
            if (existing != null && !existing.getId().equals(u.getId())) {
                throw new IllegalArgumentException("邮箱已被使用");
            }
        }
        u.setEmail(newEmail);
        userMapper.updateById(u);
    }

    @Override
    public void changePassword(String username, ChangePasswordForm form) {
        User u = findByUsername(username);
        if (u == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (!passwordEncoder.matches(form.oldPassword(), u.getPasswordHash())) {
            throw new IllegalArgumentException("旧密码不正确");
        }
        if (!form.newPassword().equals(form.confirmPassword())) {
            throw new IllegalArgumentException("两次密码输入不一致");
        }
        if (passwordEncoder.matches(form.newPassword(), u.getPasswordHash())) {
            throw new IllegalArgumentException("新密码不能与旧密码相同");
        }
        u.setPasswordHash(passwordEncoder.encode(form.newPassword()));
        userMapper.updateById(u);
    }

    @Override
    public IPage<User> search(UserQuery q, long page, long size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.username())) {
            wrapper.like(User::getUsername, q.username().trim());
        }
        if (StringUtils.hasText(q.email())) {
            wrapper.like(User::getEmail, q.email().trim());
        }
        if (q.enabled() != null) {
            wrapper.eq(User::getEnabled, q.enabled());
        }
        wrapper.orderByDesc(User::getCreatedAt);
        return userMapper.selectPage(Page.of(page, size), wrapper);
    }

    @Override
    public void toggleEnabled(Long id, Long currentUserId) {
        User u = getByIdOrThrow(id);
        if (currentUserId != null && u.getId().equals(currentUserId)) {
            throw new IllegalArgumentException(
                "操作失败：不能禁用当前登录的账号。\n" +
                "失败原因：您正在尝试禁用自己当前正在使用的账号，这会导致您立即被系统强制登出，且无法再次登录。\n" +
                "解决方法：\n" +
                "1. 如果需要禁用该账号，请先使用其他管理员账号登录后再操作\n" +
                "2. 如果是误操作，请忽略此提示并继续管理其他用户"
            );
        }
        u.setEnabled(u.getEnabled() == 1 ? 0 : 1);
        userMapper.updateById(u);
    }
}

