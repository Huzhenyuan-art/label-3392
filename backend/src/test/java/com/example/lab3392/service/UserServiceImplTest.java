package com.example.lab3392.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lab3392.entity.Role;
import com.example.lab3392.entity.User;
import com.example.lab3392.entity.UserRole;
import com.example.lab3392.mapper.RoleMapper;
import com.example.lab3392.mapper.UserMapper;
import com.example.lab3392.mapper.UserRoleMapper;
import com.example.lab3392.testsupport.DbTestSupport;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceImplTest extends DbTestSupport {
    @Autowired
    UserService userService;

    @Autowired
    UserMapper userMapper;

    @Autowired
    RoleMapper roleMapper;

    @Autowired
    UserRoleMapper userRoleMapper;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void register_createsUser_andAssignsUserRole_andEncryptsPassword() {
        Role userRole = new Role();
        userRole.setCode("USER");
        userRole.setName("普通用户");
        roleMapper.insert(userRole);

        userService.register("alice", "alice@example.com", "123456");

        User u = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "alice"));
        assertThat(u).isNotNull();
        assertThat(u.getEmail()).isEqualTo("alice@example.com");
        assertThat(u.getEnabled()).isEqualTo(1);
        assertThat(u.getPasswordHash()).isNotBlank();
        assertThat(u.getPasswordHash()).isNotEqualTo("123456");
        assertThat(passwordEncoder.matches("123456", u.getPasswordHash())).isTrue();

        List<UserRole> urs = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, u.getId()));
        assertThat(urs).hasSize(1);
        assertThat(urs.get(0).getRoleId()).isEqualTo(userRole.getId());
    }

    @Test
    void register_rejectsDuplicateUsername() {
        Role userRole = new Role();
        userRole.setCode("USER");
        userRole.setName("普通用户");
        roleMapper.insert(userRole);

        userService.register("bob", "bob@example.com", "123456");
        assertThatThrownBy(() -> userService.register("bob", "bob2@example.com", "123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户名");
    }

    @Test
    void register_rejectsDuplicateEmail() {
        Role userRole = new Role();
        userRole.setCode("USER");
        userRole.setName("普通用户");
        roleMapper.insert(userRole);

        userService.register("carl", "carl@example.com", "123456");
        assertThatThrownBy(() -> userService.register("carl2", "carl@example.com", "123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("邮箱");
    }
}
