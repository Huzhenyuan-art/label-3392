package com.example.lab3392.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lab3392.entity.Role;
import com.example.lab3392.entity.User;
import com.example.lab3392.entity.UserRole;
import com.example.lab3392.mapper.RoleMapper;
import com.example.lab3392.mapper.UserMapper;
import com.example.lab3392.mapper.UserRoleMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthUserDetailsService implements UserDetailsService {
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthUserDetailsService(UserMapper userMapper, UserRoleMapper userRoleMapper, RoleMapper roleMapper,
            PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (u == null) {
            return org.springframework.security.core.userdetails.User.builder()
                    .username(username)
                    .password(passwordEncoder.encode("__non_existent_user_password__"))
                    .authorities(new ArrayList<>())
                    .build();
        }

        List<UserRole> urs = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, u.getId()));
        List<GrantedAuthority> auth = new ArrayList<>();
        for (UserRole ur : urs) {
            Role r = roleMapper.selectById(ur.getRoleId());
            if (r != null && r.getCode() != null) auth.add(new SimpleGrantedAuthority("ROLE_" + r.getCode()));
        }

        boolean enabled = (u.getEnabled() != null && u.getEnabled() == 1);
        return org.springframework.security.core.userdetails.User.builder()
                .username(u.getUsername())
                .password(u.getPasswordHash())
                .authorities(auth)
                .disabled(!enabled)
                .build();
    }
}

