package com.example.lab3392.init;

import com.example.lab3392.entity.Product;
import com.example.lab3392.entity.ProductCategory;
import com.example.lab3392.entity.Role;
import com.example.lab3392.entity.User;
import com.example.lab3392.entity.UserRole;
import com.example.lab3392.mapper.ProductCategoryMapper;
import com.example.lab3392.mapper.ProductMapper;
import com.example.lab3392.mapper.RoleMapper;
import com.example.lab3392.mapper.UserMapper;
import com.example.lab3392.mapper.UserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

@Component
@Profile("!test")
public class DataInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleMapper roleMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final ProductMapper productMapper;
    private final ProductCategoryMapper categoryMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleMapper roleMapper, UserMapper userMapper, UserRoleMapper userRoleMapper, ProductMapper productMapper, ProductCategoryMapper categoryMapper, PasswordEncoder passwordEncoder) {
        this.roleMapper = roleMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        Role adminRole = ensureRole("ADMIN", "管理员");
        Role userRole = ensureRole("USER", "普通用户");

        User admin = ensureUser("admin", "admin@example.com", "123456");
        User user = ensureUser("user", "user@example.com", "123456");

        ensureUserRole(admin.getId(), adminRole.getId());
        ensureUserRole(admin.getId(), userRole.getId());
        ensureUserRole(user.getId(), userRole.getId());

        ensureCategories();
        ensureProducts();
        log.info("Seed data ready.");
    }

    private Role ensureRole(String code, String name) {
        Role existing = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, code));
        if (existing != null) return existing;
        Role r = new Role();
        r.setCode(code);
        r.setName(name);
        roleMapper.insert(r);
        return r;
    }

    private User ensureUser(String username, String email, String rawPassword) {
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (existing != null) return existing;
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setEnabled(1);
        userMapper.insert(u);
        return u;
    }

    private void ensureUserRole(Long userId, Long roleId) {
        UserRole existing = userRoleMapper.selectOne(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRoleId, roleId));
        if (existing != null) return;
        UserRole ur = new UserRole();
        ur.setUserId(userId);
        ur.setRoleId(roleId);
        userRoleMapper.insert(ur);
    }

    private void ensureProducts() {
        List<ProductCategory> activeCategories = categoryMapper.selectList(
                new LambdaQueryWrapper<ProductCategory>().eq(ProductCategory::getStatus, "ACTIVE")
        );
        if (activeCategories.isEmpty()) {
            log.warn("No active categories found, skipping product initialization");
            return;
        }
        Long laptopCatId = findCategoryIdByCode(activeCategories, "LAPTOP");
        Long accessoryCatId = findCategoryIdByCode(activeCategories, "ACCESSORY");
        Long monitorCatId = findCategoryIdByCode(activeCategories, "MONITOR");
        Long mobileCatId = findCategoryIdByCode(activeCategories, "MOBILE");
        Long defaultCatId = activeCategories.get(0).getId();

        List<Product> base = List.of(
                build(laptopCatId, "Aurora Pro", "轻量科技风笔记本，适合开发与演示。", new BigDecimal("6999.00"), 30, "ACTIVE"),
                build(accessoryCatId, "Neon Dock", "多口扩展坞，稳定供电与高速传输。", new BigDecimal("399.00"), 120, "ACTIVE"),
                build(accessoryCatId, "Quantum Mouse", "低延迟电竞鼠标，舒适手感。", new BigDecimal("259.00"), 80, "ACTIVE"),
                build(accessoryCatId, "Cloud Keyboard", "静音机械键盘，办公效率提升。", new BigDecimal("499.00"), 55, "ACTIVE"),
                build(monitorCatId, "Pulse Monitor", "超宽屏显示器，沉浸式工作流。", new BigDecimal("1999.00"), 18, "INACTIVE")
        );
        for (Product p : base) {
            if (productMapper.selectCount(new LambdaQueryWrapper<Product>().eq(Product::getName, p.getName())) == 0) {
                productMapper.insert(p);
            }
        }

        for (int i = 6; i <= 85; i++) {
            String name = "Tech Item " + i;
            if (productMapper.selectCount(new LambdaQueryWrapper<Product>().eq(Product::getName, name)) > 0) continue;

            Long categoryId = (i % 4 == 0) ? mobileCatId : (i % 3 == 0 ? monitorCatId : (i % 2 == 0 ? accessoryCatId : laptopCatId));
            if (categoryId == null) categoryId = defaultCatId;

            String status = (i % 7 == 0) ? "INACTIVE" : "ACTIVE";
            BigDecimal price = new BigDecimal(String.format("%d.00", 99 + (i * 37) % 3900));
            int stock = 10 + (i * 3) % 200;
            productMapper.insert(build(
                    categoryId,
                    name,
                    "演示数据条目 #" + i + "（用于分页与查询测试）。",
                    price,
                    stock,
                    status
            ));
        }
    }

    private Long findCategoryIdByCode(List<ProductCategory> categories, String code) {
        return categories.stream()
                .filter(c -> code.equals(c.getCode()))
                .map(ProductCategory::getId)
                .findFirst()
                .orElse(null);
    }

    private static Product build(Long categoryId, String name, String desc, BigDecimal price, int stock, String status) {
        Product p = new Product();
        p.setCategoryId(categoryId);
        p.setName(name);
        p.setDescription(desc);
        p.setPrice(price);
        p.setStock(stock);
        p.setStatus(status);
        return p;
    }

    private void ensureCategories() {
        List<ProductCategory> base = List.of(
                buildCategory("笔记本电脑", "LAPTOP", "高性能轻薄本与游戏本", 1, "ACTIVE"),
                buildCategory("外设配件", "ACCESSORY", "鼠标、键盘、扩展坞等", 2, "ACTIVE"),
                buildCategory("显示器", "MONITOR", "办公与电竞显示器", 3, "ACTIVE"),
                buildCategory("手机数码", "MOBILE", "智能手机与数码配件", 4, "ACTIVE"),
                buildCategory("智能穿戴", "WEARABLE", "智能手表、手环等", 5, "INACTIVE")
        );
        for (ProductCategory c : base) {
            if (categoryMapper.selectCount(new LambdaQueryWrapper<ProductCategory>().eq(ProductCategory::getCode, c.getCode())) == 0) {
                categoryMapper.insert(c);
            }
        }

        for (int i = 6; i <= 35; i++) {
            String code = "CAT" + i;
            if (categoryMapper.selectCount(new LambdaQueryWrapper<ProductCategory>().eq(ProductCategory::getCode, code)) > 0) continue;

            String status = (i % 8 == 0) ? "INACTIVE" : "ACTIVE";
            categoryMapper.insert(buildCategory(
                    "分类 " + i,
                    code,
                    "演示分类条目 #" + i + "（用于分页与查询测试）。",
                    i,
                    status
            ));
        }
    }

    private static ProductCategory buildCategory(String name, String code, String desc, int sortOrder, String status) {
        ProductCategory c = new ProductCategory();
        c.setName(name);
        c.setCode(code);
        c.setDescription(desc);
        c.setSortOrder(sortOrder);
        c.setStatus(status);
        return c;
    }
}
