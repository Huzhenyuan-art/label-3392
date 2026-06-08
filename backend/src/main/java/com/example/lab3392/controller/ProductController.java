package com.example.lab3392.controller;

import com.example.lab3392.dto.ProductForm;
import com.example.lab3392.dto.ProductQuery;
import com.example.lab3392.entity.Product;
import com.example.lab3392.entity.User;
import com.example.lab3392.service.OperationLogService;
import com.example.lab3392.service.ProductCategoryService;
import com.example.lab3392.service.ProductService;
import com.example.lab3392.service.UserService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.Objects;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProductController {
    private final ProductService productService;
    private final ProductCategoryService categoryService;
    private final OperationLogService operationLogService;
    private final UserService userService;

    public ProductController(ProductService productService, ProductCategoryService categoryService,
                             OperationLogService operationLogService, UserService userService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.operationLogService = operationLogService;
        this.userService = userService;
    }

    @GetMapping("/products")
    public String list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String minPrice,
            @RequestParam(required = false) String maxPrice,
            @RequestParam(defaultValue = "1") long page,
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

        ProductQuery q = new ProductQuery(name, priceError == null ? minVal : null, priceError == null ? maxVal : null);
        model.addAttribute("q", q);
        long safePage = priceError == null ? page : 1;
        model.addAttribute("page", productService.search(q, safePage, 10));
        model.addAttribute("username", principal != null ? principal.getName() : "");
        model.addAttribute("nameRaw", Objects.toString(name, ""));
        model.addAttribute("minPriceRaw", Objects.toString(minPrice, ""));
        model.addAttribute("maxPriceRaw", Objects.toString(maxPrice, ""));
        model.addAttribute("priceError", priceError);
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
    public String createForm(Model model) {
        model.addAttribute("mode", "create");
        model.addAttribute("form", ProductForm.empty());
        model.addAttribute("categories", categoryService.listAllActive());
        return "products/form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/products")
    public String create(@Valid @ModelAttribute("form") ProductForm form, BindingResult binding, Model model, RedirectAttributes ra, Principal principal) {
        if (binding.hasErrors()) {
            model.addAttribute("mode", "create");
            model.addAttribute("categories", categoryService.listAllActive());
            model.addAttribute("error", binding.getAllErrors().isEmpty() ? "表单校验失败" : binding.getAllErrors().get(0).getDefaultMessage());
            return "products/form";
        }
        try {
            Product created = productService.create(form);
            User operator = getCurrentOperator(principal);
            if (operator != null) {
                operationLogService.logProductCreate(created, operator.getId(), operator.getUsername());
            }
        } catch (IllegalArgumentException ex) {
            model.addAttribute("mode", "create");
            model.addAttribute("categories", categoryService.listAllActive());
            model.addAttribute("error", ex.getMessage());
            return "products/form";
        }
        ra.addFlashAttribute("flashOk", "创建成功");
        return "redirect:/products";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/products/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Product p = productService.getByIdOrThrow(id);
        model.addAttribute("mode", "edit");
        model.addAttribute("form", ProductForm.fromEntity(p));
        model.addAttribute("categories", categoryService.listAllActive());
        return "products/form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/products/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") ProductForm form, BindingResult binding, Model model, RedirectAttributes ra, Principal principal) {
        if (binding.hasErrors()) {
            model.addAttribute("mode", "edit");
            model.addAttribute("categories", categoryService.listAllActive());
            model.addAttribute("error", binding.getAllErrors().isEmpty() ? "表单校验失败" : binding.getAllErrors().get(0).getDefaultMessage());
            return "products/form";
        }
        try {
            Product before = productService.getByIdOrThrow(id);
            productService.update(id, form);
            Product after = productService.getByIdOrThrow(id);
            User operator = getCurrentOperator(principal);
            if (operator != null) {
                operationLogService.logProductUpdate(before, after, operator.getId(), operator.getUsername());
            }
        } catch (IllegalArgumentException ex) {
            model.addAttribute("mode", "edit");
            model.addAttribute("categories", categoryService.listAllActive());
            model.addAttribute("error", ex.getMessage());
            return "products/form";
        }
        ra.addFlashAttribute("flashOk", "更新成功");
        return "redirect:/products";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/products/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra, Principal principal) {
        Product existing = productService.getByIdOrThrow(id);
        productService.delete(id);
        User operator = getCurrentOperator(principal);
        if (operator != null) {
            operationLogService.logProductDelete(existing, operator.getId(), operator.getUsername());
        }
        ra.addFlashAttribute("flashOk", "删除成功");
        return "redirect:/products";
    }

    private User getCurrentOperator(Principal principal) {
        if (principal == null) return null;
        return userService.findByUsername(principal.getName());
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
