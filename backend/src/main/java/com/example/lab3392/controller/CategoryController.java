package com.example.lab3392.controller;

import com.example.lab3392.dto.CategoryForm;
import com.example.lab3392.dto.CategoryQuery;
import com.example.lab3392.entity.ProductCategory;
import com.example.lab3392.service.ProductCategoryService;
import jakarta.validation.Valid;
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
public class CategoryController {
    private final ProductCategoryService categoryService;

    public CategoryController(ProductCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/categories")
    public String list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(defaultValue = "1") long page,
            Model model,
            Principal principal
    ) {
        CategoryQuery q = new CategoryQuery(name, code);
        model.addAttribute("q", q);
        model.addAttribute("page", categoryService.search(q, page, 10));
        model.addAttribute("username", principal != null ? principal.getName() : "");
        model.addAttribute("nameRaw", Objects.toString(name, ""));
        model.addAttribute("codeRaw", Objects.toString(code, ""));
        return "categories/list";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/categories/new")
    public String createForm(Model model) {
        model.addAttribute("mode", "create");
        model.addAttribute("form", CategoryForm.empty());
        return "categories/form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/categories")
    public String create(@Valid @ModelAttribute("form") CategoryForm form, BindingResult binding, Model model, RedirectAttributes ra) {
        if (binding.hasErrors()) {
            model.addAttribute("mode", "create");
            model.addAttribute("error", binding.getAllErrors().isEmpty() ? "表单校验失败" : binding.getAllErrors().get(0).getDefaultMessage());
            return "categories/form";
        }
        categoryService.create(form);
        ra.addFlashAttribute("flashOk", "创建成功");
        return "redirect:/categories";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/categories/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        ProductCategory c = categoryService.getByIdOrThrow(id);
        model.addAttribute("mode", "edit");
        model.addAttribute("form", CategoryForm.fromEntity(c));
        return "categories/form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/categories/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") CategoryForm form, BindingResult binding, Model model, RedirectAttributes ra) {
        if (binding.hasErrors()) {
            model.addAttribute("mode", "edit");
            model.addAttribute("error", binding.getAllErrors().isEmpty() ? "表单校验失败" : binding.getAllErrors().get(0).getDefaultMessage());
            return "categories/form";
        }
        categoryService.update(id, form);
        ra.addFlashAttribute("flashOk", "更新成功");
        return "redirect:/categories";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/categories/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        categoryService.delete(id);
        ra.addFlashAttribute("flashOk", "删除成功");
        return "redirect:/categories";
    }
}
