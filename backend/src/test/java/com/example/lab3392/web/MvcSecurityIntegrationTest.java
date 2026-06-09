package com.example.lab3392.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lab3392.entity.OperationLog;
import com.example.lab3392.entity.Product;
import com.example.lab3392.entity.ProductCategory;
import com.example.lab3392.entity.Role;
import com.example.lab3392.entity.User;
import com.example.lab3392.entity.UserRole;
import com.example.lab3392.mapper.OperationLogMapper;
import com.example.lab3392.mapper.ProductCategoryMapper;
import com.example.lab3392.mapper.ProductMapper;
import com.example.lab3392.mapper.RoleMapper;
import com.example.lab3392.mapper.UserMapper;
import com.example.lab3392.mapper.UserRoleMapper;
import com.example.lab3392.testsupport.DbTestSupport;
import java.math.BigDecimal;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MvcSecurityIntegrationTest extends DbTestSupport {
    @Autowired
    org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    UserMapper userMapper;

    @Autowired
    RoleMapper roleMapper;

    @Autowired
    UserRoleMapper userRoleMapper;

    @Autowired
    ProductMapper productMapper;

    @Autowired
    ProductCategoryMapper categoryMapper;

    @Autowired
    OperationLogMapper operationLogMapper;

    Long testCategoryId;

    @BeforeEach
    void setUp() {
        testCategoryId = null;
    }

    Long ensureTestCategory() {
        if (testCategoryId != null) return testCategoryId;
        ProductCategory c = new ProductCategory();
        c.setName("测试分类");
        c.setCode("TEST_CAT");
        c.setSortOrder(1);
        c.setStatus("ACTIVE");
        categoryMapper.insert(c);
        testCategoryId = c.getId();
        return testCategoryId;
    }

    @Test
    void anonymous_isRedirectedToLogin_whenAccessingProtectedPage() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void register_flow_createsUser_andRedirectsToLogin() throws Exception {
        ensureRole("USER", "普通用户");

        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "alice")
                        .param("email", "alice@example.com")
                        .param("password", "123456")
                        .param("confirmPassword", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        User u = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "alice"));
        assertThat(u).isNotNull();
        assertThat(passwordEncoder.matches("123456", u.getPasswordHash())).isTrue();

        Role userRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, "USER"));
        assertThat(userRole).isNotNull();
        assertThat(userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, u.getId())
                .eq(UserRole::getRoleId, userRole.getId()))).isEqualTo(1);
    }

    @Test
    void login_withRememberMe_setsCookie_andAllowsAdminCrud() throws Exception {
        Role admin = ensureRole("ADMIN", "管理员");
        Role userRole = ensureRole("USER", "普通用户");
        Long catId = ensureTestCategory();

        User u = new User();
        u.setUsername("admin");
        u.setEmail("admin@example.com");
        u.setEnabled(1);
        u.setPasswordHash(passwordEncoder.encode("123456"));
        userMapper.insert(u);
        ensureUserRole(u.getId(), admin.getId());
        ensureUserRole(u.getId(), userRole.getId());

        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "admin")
                        .param("password", "123456")
                        .param("remember-me", "on"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(header().stringValues("Set-Cookie", hasItem(containsString("remember-me="))))
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/products/new").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/products").session(session).with(csrf())
                        .param("categoryId", catId.toString())
                        .param("name", "Test Product")
                        .param("description", "desc")
                        .param("price", "9.99")
                        .param("stock", "1")
                        .param("status", "ACTIVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));

        Product p = productMapper.selectOne(new LambdaQueryWrapper<Product>().eq(Product::getName, "Test Product"));
        assertThat(p).isNotNull();
        assertThat(p.getPrice()).isEqualByComparingTo(new BigDecimal("9.99"));
        assertThat(p.getCategoryId()).isEqualTo(catId);
    }

    @Test
    void csrf_isRequired_forStateChangingRequests() throws Exception {
        Role admin = ensureRole("ADMIN", "管理员");
        Role userRole = ensureRole("USER", "普通用户");
        Long catId = ensureTestCategory();

        User u = new User();
        u.setUsername("admin");
        u.setEmail("admin@example.com");
        u.setEnabled(1);
        u.setPasswordHash(passwordEncoder.encode("123456"));
        userMapper.insert(u);
        ensureUserRole(u.getId(), admin.getId());
        ensureUserRole(u.getId(), userRole.getId());

        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "admin")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(post("/products").session(session)
                        .param("categoryId", catId.toString())
                        .param("name", "No Csrf")
                        .param("description", "desc")
                        .param("price", "1.00")
                        .param("stock", "1")
                        .param("status", "ACTIVE"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rememberMe_cookie_allowsAccessWithoutSession() throws Exception {
        Role admin = ensureRole("ADMIN", "管理员");
        Role userRole = ensureRole("USER", "普通用户");

        User u = new User();
        u.setUsername("admin");
        u.setEmail("admin@example.com");
        u.setEnabled(1);
        u.setPasswordHash(passwordEncoder.encode("123456"));
        userMapper.insert(u);
        ensureUserRole(u.getId(), admin.getId());
        ensureUserRole(u.getId(), userRole.getId());

        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "admin")
                        .param("password", "123456")
                        .param("remember-me", "on"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        Cookie remember = login.getResponse().getCookie("remember-me");
        assertThat(remember).isNotNull();

        mockMvc.perform(get("/products").cookie(remember))
                .andExpect(status().isOk());
    }

    @Test
    void rememberMe_notChecked_clearsExistingRememberMeCookie() throws Exception {
        Role admin = ensureRole("ADMIN", "管理员");
        Role userRole = ensureRole("USER", "普通用户");

        User u = new User();
        u.setUsername("admin");
        u.setEmail("admin@example.com");
        u.setEnabled(1);
        u.setPasswordHash(passwordEncoder.encode("123456"));
        userMapper.insert(u);
        ensureUserRole(u.getId(), admin.getId());
        ensureUserRole(u.getId(), userRole.getId());

        MvcResult r = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "admin")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        Cookie cleared = r.getResponse().getCookie("remember-me");
        assertThat(cleared).isNotNull();
        assertThat(cleared.getMaxAge()).isEqualTo(0);
    }

    @Test
    void nonAdmin_cannotAccessAdminPages() throws Exception {
        Role userRole = ensureRole("USER", "普通用户");

        User u = new User();
        u.setUsername("user");
        u.setEmail("user@example.com");
        u.setEnabled(1);
        u.setPasswordHash(passwordEncoder.encode("123456"));
        userMapper.insert(u);
        ensureUserRole(u.getId(), userRole.getId());

        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "user")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/products/new").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void products_list_usesInternalPagination_size10() throws Exception {
        Role admin = ensureRole("ADMIN", "管理员");
        Role userRole = ensureRole("USER", "普通用户");
        Long catId = ensureTestCategory();

        User u = new User();
        u.setUsername("admin");
        u.setEmail("admin@example.com");
        u.setEnabled(1);
        u.setPasswordHash(passwordEncoder.encode("123456"));
        userMapper.insert(u);
        ensureUserRole(u.getId(), admin.getId());
        ensureUserRole(u.getId(), userRole.getId());

        for (int i = 1; i <= 25; i++) {
            Product p = new Product();
            p.setCategoryId(catId);
            p.setName("Product " + i);
            p.setDescription("D" + i);
            p.setPrice(new BigDecimal("1.00"));
            p.setStock(1);
            p.setStatus("ACTIVE");
            productMapper.insert(p);
        }

        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "admin")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        MvcResult r = mockMvc.perform(get("/products?page=2").session(session))
                .andExpect(status().isOk())
                .andReturn();

        String html = r.getResponse().getContentAsString();
        assertThat(html).contains("当前第 2 / 3 页");
        assertThat(countOccurrences(html, "data-confirm=\"确定删除该产品吗？\"")).isEqualTo(10);
    }

    @Test
    void products_detail_page_showsProduct() throws Exception {
        Role admin = ensureRole("ADMIN", "管理员");
        Role userRole = ensureRole("USER", "普通用户");
        Long catId = ensureTestCategory();

        User u = new User();
        u.setUsername("admin");
        u.setEmail("admin@example.com");
        u.setEnabled(1);
        u.setPasswordHash(passwordEncoder.encode("123456"));
        userMapper.insert(u);
        ensureUserRole(u.getId(), admin.getId());
        ensureUserRole(u.getId(), userRole.getId());

        Product p = new Product();
        p.setCategoryId(catId);
        p.setName("Detail Product");
        p.setDescription("detail desc");
        p.setPrice(new BigDecimal("12.34"));
        p.setStock(7);
        p.setStatus("ACTIVE");
        productMapper.insert(p);

        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "admin")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        MvcResult r = mockMvc.perform(get("/products/" + p.getId()).session(session))
                .andExpect(status().isOk())
                .andReturn();

        String html = r.getResponse().getContentAsString();
        assertThat(html).contains("产品详情");
        assertThat(html).contains("Detail Product");
    }

    @Test
    void products_list_validatesPriceRange_pairRequired_andNumeric() throws Exception {
        Role admin = ensureRole("ADMIN", "管理员");
        Role userRole = ensureRole("USER", "普通用户");

        User u = new User();
        u.setUsername("admin");
        u.setEmail("admin@example.com");
        u.setEnabled(1);
        u.setPasswordHash(passwordEncoder.encode("123456"));
        userMapper.insert(u);
        ensureUserRole(u.getId(), admin.getId());
        ensureUserRole(u.getId(), userRole.getId());

        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "admin")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        MvcResult r1 = mockMvc.perform(get("/products?minPrice=60").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(r1.getResponse().getContentAsString()).contains("价格区间需同时填写");

        MvcResult r2 = mockMvc.perform(get("/products?minPrice=abc&maxPrice=100").session(session))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(r2.getResponse().getContentAsString()).contains("价格区间请输入数字");
    }

    @Test
    void pagination_links_preserve_raw_query_strings() throws Exception {
        Role admin = ensureRole("ADMIN", "管理员");
        Role userRole = ensureRole("USER", "普通用户");
        Long catId = ensureTestCategory();

        User u = new User();
        u.setUsername("admin");
        u.setEmail("admin@example.com");
        u.setEnabled(1);
        u.setPasswordHash(passwordEncoder.encode("123456"));
        userMapper.insert(u);
        ensureUserRole(u.getId(), admin.getId());
        ensureUserRole(u.getId(), userRole.getId());

        for (int i = 1; i <= 15; i++) {
            Product p = new Product();
            p.setCategoryId(catId);
            p.setName("TestProduct " + i);
            p.setDescription("D" + i);
            p.setPrice(new BigDecimal("50.00"));
            p.setStock(1);
            p.setStatus("ACTIVE");
            productMapper.insert(p);
        }

        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "admin")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        MvcResult r1 = mockMvc.perform(get("/products?name=TestProduct&minPrice=10.0&maxPrice=100.00&page=1").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String html1 = r1.getResponse().getContentAsString();
        assertThat(html1).contains("name=TestProduct");
        assertThat(html1).contains("minPrice=10.0&amp;");
        assertThat(html1).contains("maxPrice=100.00&amp;");
        assertThat(html1).doesNotContain("minPrice=10&amp;");
        assertThat(html1).doesNotContain("maxPrice=100.0&amp;");

        MvcResult r2 = mockMvc.perform(get("/products?name=&minPrice=abc&maxPrice=xyz&page=1").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String html2 = r2.getResponse().getContentAsString();
        assertThat(html2).contains("minPrice=abc");
        assertThat(html2).contains("maxPrice=xyz");
        assertThat(html2).contains("价格区间请输入数字");
    }

    @Test
    void login_disabledUser_showsDisabledMessage() throws Exception {
        Role userRole = ensureRole("USER", "普通用户");

        User u = new User();
        u.setUsername("disabled");
        u.setEmail("disabled@example.com");
        u.setEnabled(0);
        u.setPasswordHash(passwordEncoder.encode("123456"));
        userMapper.insert(u);
        ensureUserRole(u.getId(), userRole.getId());

        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "disabled")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?disabled"));

        MvcResult result = mockMvc.perform(get("/login?disabled"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("账号已被禁用，请联系管理员。");
    }

    @Test
    void login_badCredentials_showsGenericError_notLeakingExistence() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "nonexistent")
                        .param("password", "wrongpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        MvcResult result1 = mockMvc.perform(get("/login?error"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(result1.getResponse().getContentAsString()).contains("用户名或密码错误，请重试。");
        assertThat(result1.getResponse().getContentAsString()).doesNotContain("账号已被禁用");

        Role userRole = ensureRole("USER", "普通用户");
        User u = new User();
        u.setUsername("existing");
        u.setEmail("existing@example.com");
        u.setEnabled(1);
        u.setPasswordHash(passwordEncoder.encode("correctpass"));
        userMapper.insert(u);
        ensureUserRole(u.getId(), userRole.getId());

        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "existing")
                        .param("password", "wrongpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        MvcResult result2 = mockMvc.perform(get("/login?error"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(result2.getResponse().getContentAsString()).contains("用户名或密码错误，请重试。");
        assertThat(result2.getResponse().getContentAsString()).doesNotContain("账号已被禁用");
    }

    @Test
    void operationLogs_page_userRole_accessDenied() throws Exception {
        Role userRole = ensureRole("USER", "普通用户");
        User u = new User();
        u.setUsername("testuser_logs");
        u.setEmail("testuser_logs@example.com");
        u.setPasswordHash(passwordEncoder.encode("password123"));
        u.setEnabled(1);
        userMapper.insert(u);
        ensureUserRole(u.getId(), userRole.getId());

        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "testuser_logs")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/operation-logs").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void operationLogs_page_adminRole_accessOk() throws Exception {
        Role adminRole = ensureRole("ADMIN", "管理员");
        User u = new User();
        u.setUsername("admin_logs");
        u.setEmail("admin_logs@example.com");
        u.setPasswordHash(passwordEncoder.encode("password123"));
        u.setEnabled(1);
        userMapper.insert(u);
        ensureUserRole(u.getId(), adminRole.getId());

        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "admin_logs")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        MvcResult r = mockMvc.perform(get("/operation-logs").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String html = r.getResponse().getContentAsString();
        assertThat(html).contains("操作审计日志");
        assertThat(html).contains("操作类型");
        assertThat(html).contains("时间区间");
    }

    @Test
    void userRole_forbiddenPostCreateProduct() throws Exception {
        Role userRole = ensureRole("USER", "普通用户");
        Long catId = ensureTestCategory();

        User u = new User();
        u.setUsername("plainuser");
        u.setEmail("plainuser@example.com");
        u.setEnabled(1);
        u.setPasswordHash(passwordEncoder.encode("123456"));
        userMapper.insert(u);
        ensureUserRole(u.getId(), userRole.getId());

        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "plainuser")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(post("/products").session(session).with(csrf())
                        .param("categoryId", catId.toString())
                        .param("name", "Should Fail")
                        .param("description", "desc")
                        .param("price", "9.99")
                        .param("stock", "1")
                        .param("status", "ACTIVE"))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_deleteProduct_withCsrf_success() throws Exception {
        Role admin = ensureRole("ADMIN", "管理员");
        Role userRole = ensureRole("USER", "普通用户");
        Long catId = ensureTestCategory();

        User u = new User();
        u.setUsername("admin_del");
        u.setEmail("admin_del@example.com");
        u.setEnabled(1);
        u.setPasswordHash(passwordEncoder.encode("123456"));
        userMapper.insert(u);
        ensureUserRole(u.getId(), admin.getId());
        ensureUserRole(u.getId(), userRole.getId());

        Product p = new Product();
        p.setCategoryId(catId);
        p.setName("ToDelete");
        p.setDescription("will be deleted");
        p.setPrice(new BigDecimal("19.99"));
        p.setStock(5);
        p.setStatus("ACTIVE");
        productMapper.insert(p);
        Long productId = p.getId();

        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "admin_del")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(post("/products/" + productId + "/delete").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));

        assertThat(productMapper.selectById(productId)).isNull();
    }

    @Test
    void products_list_minPriceOnly_showsPriceError_pageRevertsTo1() throws Exception {
        Role admin = ensureRole("ADMIN", "管理员");
        Role userRole = ensureRole("USER", "普通用户");
        Long catId = ensureTestCategory();

        User u = new User();
        u.setUsername("admin_price1");
        u.setEmail("admin_price1@example.com");
        u.setEnabled(1);
        u.setPasswordHash(passwordEncoder.encode("123456"));
        userMapper.insert(u);
        ensureUserRole(u.getId(), admin.getId());
        ensureUserRole(u.getId(), userRole.getId());

        for (int i = 1; i <= 15; i++) {
            Product p = new Product();
            p.setCategoryId(catId);
            p.setName("PItem " + i);
            p.setPrice(new BigDecimal("10.00"));
            p.setStock(1);
            p.setStatus("ACTIVE");
            productMapper.insert(p);
        }

        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "admin_price1")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        MvcResult r = mockMvc.perform(get("/products?minPrice=50&page=3").session(session))
                .andExpect(status().isOk())
                .andReturn();

        String html = r.getResponse().getContentAsString();
        assertThat(html).contains("价格区间需同时填写");
        assertThat(html).contains("当前第 1 /");
    }

    @Test
    void products_list_maxPriceOnly_showsPriceError_pageRevertsTo1() throws Exception {
        Role admin = ensureRole("ADMIN", "管理员");
        Role userRole = ensureRole("USER", "普通用户");
        Long catId = ensureTestCategory();

        User u = new User();
        u.setUsername("admin_price2");
        u.setEmail("admin_price2@example.com");
        u.setEnabled(1);
        u.setPasswordHash(passwordEncoder.encode("123456"));
        userMapper.insert(u);
        ensureUserRole(u.getId(), admin.getId());
        ensureUserRole(u.getId(), userRole.getId());

        for (int i = 1; i <= 15; i++) {
            Product p = new Product();
            p.setCategoryId(catId);
            p.setName("QItem " + i);
            p.setPrice(new BigDecimal("10.00"));
            p.setStock(1);
            p.setStatus("ACTIVE");
            productMapper.insert(p);
        }

        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "admin_price2")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        MvcResult r = mockMvc.perform(get("/products?maxPrice=200&page=5").session(session))
                .andExpect(status().isOk())
                .andReturn();

        String html = r.getResponse().getContentAsString();
        assertThat(html).contains("价格区间需同时填写");
        assertThat(html).contains("当前第 1 /");
    }

    @Test
    void product_creates_operationLog_recorded() throws Exception {
        Role adminRole = ensureRole("ADMIN", "管理员");
        User u = new User();
        u.setUsername("admin_createlog");
        u.setEmail("admin_createlog@example.com");
        u.setPasswordHash(passwordEncoder.encode("password123"));
        u.setEnabled(1);
        userMapper.insert(u);
        ensureUserRole(u.getId(), adminRole.getId());

        Long catId = ensureTestCategory();

        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "admin_createlog")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        long logCountBefore = operationLogMapper.selectCount(new LambdaQueryWrapper<>());

        mockMvc.perform(post("/products").with(csrf()).session(session)
                        .param("categoryId", catId.toString())
                        .param("name", "审计测试产品")
                        .param("description", "测试描述")
                        .param("price", "99.99")
                        .param("stock", "100")
                        .param("status", "ACTIVE"))
                .andExpect(status().is3xxRedirection());

        long logCountAfter = operationLogMapper.selectCount(new LambdaQueryWrapper<>());
        assertThat(logCountAfter).isEqualTo(logCountBefore + 1);

        OperationLog log = operationLogMapper.selectOne(new LambdaQueryWrapper<OperationLog>()
                .eq(OperationLog::getOperationType, "CREATE")
                .eq(OperationLog::getOperatorUsername, "admin_createlog")
                .orderByDesc(OperationLog::getId)
                .last("LIMIT 1"));
        assertThat(log).isNotNull();
        assertThat(log.getTargetType()).isEqualTo("PRODUCT");
        assertThat(log.getBeforeSnapshot()).isNull();
        assertThat(log.getAfterSnapshot()).contains("审计测试产品");
        assertThat(log.getAfterSnapshot()).contains("99.99");
    }

    private static int countOccurrences(String s, String needle) {
        int count = 0;
        int idx = 0;
        while (true) {
            int found = s.indexOf(needle, idx);
            if (found < 0) return count;
            count++;
            idx = found + needle.length();
        }
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
}
