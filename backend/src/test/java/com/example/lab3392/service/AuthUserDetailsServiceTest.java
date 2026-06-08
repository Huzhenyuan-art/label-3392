package com.example.lab3392.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.lab3392.entity.Role;
import com.example.lab3392.entity.User;
import com.example.lab3392.entity.UserRole;
import com.example.lab3392.mapper.RoleMapper;
import com.example.lab3392.mapper.UserMapper;
import com.example.lab3392.mapper.UserRoleMapper;
import com.example.lab3392.testsupport.DbTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthUserDetailsServiceTest extends DbTestSupport {
    @Autowired
    AuthUserDetailsService authUserDetailsService;

    @Autowired
    UserMapper userMapper;

    @Autowired
    RoleMapper roleMapper;

    @Autowired
    UserRoleMapper userRoleMapper;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void loadUserByUsername_returnsAuthoritiesFromDb() {
        Role admin = new Role();
        admin.setCode("ADMIN");
        admin.setName("管理员");
        roleMapper.insert(admin);

        Role userRole = new Role();
        userRole.setCode("USER");
        userRole.setName("普通用户");
        roleMapper.insert(userRole);

        User u = new User();
        u.setUsername("admin");
        u.setEmail("admin@example.com");
        u.setEnabled(1);
        u.setPasswordHash(passwordEncoder.encode("123456"));
        userMapper.insert(u);

        UserRole ur1 = new UserRole();
        ur1.setUserId(u.getId());
        ur1.setRoleId(admin.getId());
        userRoleMapper.insert(ur1);

        UserRole ur2 = new UserRole();
        ur2.setUserId(u.getId());
        ur2.setRoleId(userRole.getId());
        userRoleMapper.insert(ur2);

        UserDetails details = authUserDetailsService.loadUserByUsername("admin");
        assertThat(details.getUsername()).isEqualTo("admin");
        assertThat(details.getAuthorities()).extracting(a -> a.getAuthority())
                .contains("ROLE_ADMIN", "ROLE_USER");
    }
}

