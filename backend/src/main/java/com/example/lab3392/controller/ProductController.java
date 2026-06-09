package com.example.lab3392.controller;

import com.example.lab3392.dto.CsvImportResult;
import com.example.lab3392.dto.ProductForm;
import com.example.lab3392.dto.ProductQuery;
import com.example.lab3392.entity.Product;
import com.example.lab3392.entity.User;
import com.example.lab3392.service.CacheService;
import com.example.lab3392.service.FileStorageService;
import com.example.lab3392.service.OperationLogService;
import com.example.lab3392.service.ProductCategoryService;
import com.example.lab3392.service.ProductService;
import com.example.lab3392.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.Map;
import java.util.Objects;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProductController {
    private final ProductService productService;
    private final ProductCategoryService categoryService;
    private final OperationLogService operationLogService;
    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final CacheService cacheService;

    public ProductController(ProductService productService, ProductCategoryService categoryService,
                             OperationLogService operationLogService, UserService userService,
                             FileStorageService fileStorageService, CacheService cacheService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.operationLogService = operationLogService;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
        this.cacheService = cacheService;
    }

    @GetMapping("/products")
    public String list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String minPrice,
            @RequestParam(required = false) String maxPrice,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            Model model,
            Principal principal
    ) {
        String minRaw = trimToNull(minPrice);
        String maxRaw = trimToNull(maxPrice);

        String priceError = null;
        BigDecimal minVal = null;
        BigDecimal maxVal = null;

        boolean minPresent = minRaw != null;
        boolean maxPresent = maxRaw != null;
        if (minPresent ^ maxPresent) {
            priceError = "价格区间需同时填写最小价和最大价（或同时留空）";
        } else if (minPresent) {
            try {
                minVal = new BigDecimal(minRaw);
                maxVal = new BigDecimal(maxRaw);
                if (minVal.compareTo(maxVal) > 0) {
                    priceError = "价格区间不合法：最小价不能大于最大价";
                }
            } catch (NumberFormatException ex) {
                priceError = "价格区间请输入数字，例如：199.00";
            }
        }

        long safePageSize = (pageSize == 10 || pageSize == 20 || pageSize == 50) ? pageSize : 10;

        ProductQuery q = new ProductQuery(name, priceError == null ? minVal : null, priceError == null ? maxVal : null);
        model.addAttribute("q", q);
        long safePage = priceError == null ? page : 1;
        model.addAttribute("page", productService.search(q, safePage, safePageSize));
        model.addAttribute("username", principal != null ? principal.getName() : "");
        model.addAttribute("nameRaw", Objects.toString(name, ""));
        model.addAttribute("minPriceRaw", Objects.toString(minPrice, ""));
        model.addAttribute("maxPriceRaw", Objects.toString(maxPrice, ""));
        model.addAttribute("priceError", priceError);
        model.addAttribute("pageSize", safePageSize);
        return "products/list";
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Long id, Model model, Principal principal) {
        model.addAttribute("product", productService.getByIdWithCategory(id));
        model.addAttribute("username", principal != null ? principal.getName() : "");
        return "products/detail";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/products/new")
    public String createForm(Model model, @RequestParam(required = false) String returnUrl) {
        model.addAttribute("mode", "create");
        model.addAttribute("form", ProductForm.empty());
        model.addAttribute("categories", categoryService.listAllActive());
        model.addAttribute("returnUrl", returnUrl);
        model.addAttribute("coverImage", null);
        return "products/form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/products")
    public String create(@Valid @ModelAttribute("form") ProductForm form, BindingResult binding,
                         @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
                         Model model, RedirectAttributes ra, Principal principal,
                         @RequestParam(required = false) String returnUrl) {
        if (binding.hasErrors()) {
            model.addAttribute("mode", "create");
            model.addAttribute("categories", categoryService.listAllActive());
            model.addAttribute("error", binding.getAllErrors().isEmpty() ? "表单校验失败" : binding.getAllErrors().get(0).getDefaultMessage());
            model.addAttribute("returnUrl", returnUrl);
            model.addAttribute("coverImage", null);
            return "products/form";
        }
        try {
            Product created = productService.create(form, coverImage);
            User operator = getCurrentOperator(principal);
            if (operator != null) {
                operationLogService.logProductCreate(created, operator.getId(), operator.getUsername());
            }
        } catch (IllegalArgumentException ex) {
            model.addAttribute("mode", "create");
            model.addAttribute("categories", categoryService.listAllActive());
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("returnUrl", returnUrl);
            model.addAttribute("coverImage", null);
            return "products/form";
        }
        ra.addFlashAttribute("flashOk", "创建成功");
        return safeRedirect(returnUrl);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/products/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, @RequestParam(required = false) String returnUrl) {
        Product p = productService.getByIdOrThrow(id);
        model.addAttribute("mode", "edit");
        model.addAttribute("form", ProductForm.fromEntity(p));
        model.addAttribute("categories", categoryService.listAllActive());
        model.addAttribute("returnUrl", returnUrl);
        model.addAttribute("coverImage", p.getCoverImage());
        return "products/form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/products/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") ProductForm form, BindingResult binding,
                         @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
                         Model model, RedirectAttributes ra, Principal principal,
                         @RequestParam(required = false) String returnUrl) {
        if (binding.hasErrors()) {
            model.addAttribute("mode", "edit");
            model.addAttribute("categories", categoryService.listAllActive());
            model.addAttribute("error", binding.getAllErrors().isEmpty() ? "表单校验失败" : binding.getAllErrors().get(0).getDefaultMessage());
            model.addAttribute("returnUrl", returnUrl);
            model.addAttribute("coverImage", productService.getByIdOrThrow(id).getCoverImage());
            return "products/form";
        }
        try {
            Product before = productService.getByIdOrThrow(id);
            productService.update(id, form, coverImage);
            Product after = productService.getByIdOrThrow(id);
            User operator = getCurrentOperator(principal);
            if (operator != null) {
                operationLogService.logProductUpdate(before, after, operator.getId(), operator.getUsername());
            }
        } catch (IllegalArgumentException ex) {
            model.addAttribute("mode", "edit");
            model.addAttribute("categories", categoryService.listAllActive());
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("returnUrl", returnUrl);
            model.addAttribute("coverImage", productService.getByIdOrThrow(id).getCoverImage());
            return "products/form";
        }
        ra.addFlashAttribute("flashOk", "更新成功");
        return safeRedirect(returnUrl);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/products/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra, Principal principal, @RequestParam(required = false) String returnUrl) {
        Product existing = productService.getByIdOrThrow(id);
        productService.delete(id);
        User operator = getCurrentOperator(principal);
        if (operator != null) {
            operationLogService.logProductDelete(existing, operator.getId(), operator.getUsername());
        }
        ra.addFlashAttribute("flashOk", "删除成功");
        return safeRedirect(returnUrl);
    }

    private User getCurrentOperator(Principal principal) {
        if (principal == null) return null;
        return userService.findByUsername(principal.getName());
    }

    @GetMapping("/product-images/{filename}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) throws IOException {
        Path file = fileStorageService.resolve(filename);
        if (!Files.exists(file)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new UrlResource(file.toUri());
        String contentType = Files.probeContentType(file);
        if (contentType == null) contentType = "application/octet-stream";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/products/export")
    public void exportCsv(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String minPrice,
            @RequestParam(required = false) String maxPrice,
            HttpServletResponse response,
            Principal principal
    ) throws IOException {
        String minRaw = trimToNull(minPrice);
        String maxRaw = trimToNull(maxPrice);

        BigDecimal minVal = null;
        BigDecimal maxVal = null;

        try {
            if (minRaw != null) minVal = new BigDecimal(minRaw);
            if (maxRaw != null) maxVal = new BigDecimal(maxRaw);
        } catch (NumberFormatException ignored) {
        }

        ProductQuery q = new ProductQuery(name, minVal, maxVal);

        String fileName = URLEncoder.encode("产品列表.csv", StandardCharsets.UTF_8);
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);

        productService.exportCsv(q, response.getOutputStream());

        User operator = getCurrentOperator(principal);
        if (operator != null) {
            operationLogService.logProductExport(operator.getId(), operator.getUsername());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/products/import")
    public String importCsv(
            @RequestParam("file") MultipartFile file,
            RedirectAttributes ra,
            Principal principal,
            @RequestParam(required = false) String returnUrl
    ) {
        if (file.isEmpty()) {
            ra.addFlashAttribute("flashBad", "请选择要上传的CSV文件");
            return safeRedirect(returnUrl);
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
            ra.addFlashAttribute("flashBad", "请上传CSV格式的文件");
            return safeRedirect(returnUrl);
        }

        try {
            CsvImportResult result = productService.importCsv(file.getInputStream());
            ra.addFlashAttribute("importResult", result);

            StringBuilder message = new StringBuilder();
            message.append(String.format("导入完成：共 %d 行，成功 %d 行，失败 %d 行",
                    result.getTotalRows(), result.getSuccessCount(), result.getFailureCount()));

            if (result.hasFailures()) {
                ra.addFlashAttribute("flashBad", message.toString());
            } else {
                ra.addFlashAttribute("flashOk", message.toString());
            }

            User operator = getCurrentOperator(principal);
            if (operator != null) {
                operationLogService.logProductImport(
                        result.getTotalRows(), result.getSuccessCount(), result.getFailureCount(),
                        operator.getId(), operator.getUsername()
                );
            }

        } catch (Exception e) {
            ra.addFlashAttribute("flashBad", "导入失败：" + e.getMessage());
        }

        return safeRedirect(returnUrl);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/products/cache/refresh")
    @org.springframework.web.bind.annotation.ResponseBody
    public ResponseEntity<Map<String, String>> refreshCache() {
        cacheService.evictProductPagesV3Cache();
        return ResponseEntity.ok(Map.of("message", "productPagesV3 缓存已刷新"));
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String safeRedirect(String returnUrl) {
        if (returnUrl != null && !returnUrl.isBlank() && returnUrl.startsWith("/products")) {
            return "redirect:" + returnUrl;
        }
        return "redirect:/products";
    }
}
