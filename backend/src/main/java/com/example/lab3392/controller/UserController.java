package com.example.lab3392.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.lab3392.dto.ChangePasswordForm;
import com.example.lab3392.dto.ProfileForm;
import com.example.lab3392.dto.UserQuery;
import com.example.lab3392.entity.User;
import com.example.lab3392.service.UserService;
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
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profile(Model model, Principal principal) {
        User u = userService.findByUsername(principal.getName());
        model.addAttribute("user", u);
        model.addAttribute("username", principal.getName());
        model.addAttribute("profileForm", new ProfileForm(u.getEmail()));
        model.addAttribute("passwordForm", ChangePasswordForm.empty());
        return "users/profile";
    }

    @PostMapping("/profile/email")
    public String updateEmail(@Valid @ModelAttribute("profileForm") ProfileForm form, BindingResult binding, Model model, Principal principal, RedirectAttributes ra) {
        User u = userService.findByUsername(principal.getName());
        model.addAttribute("user", u);
        model.addAttribute("username", principal.getName());
        if (binding.hasErrors()) {
            model.addAttribute("passwordForm", ChangePasswordForm.empty());
            model.addAttribute("emailError", binding.getAllErrors().isEmpty() ? "表单校验失败" : binding.getAllErrors().get(0).getDefaultMessage());
            return "users/profile";
        }
        try {
            userService.updateEmail(principal.getName(), form);
            ra.addFlashAttribute("flashOk", "邮箱修改成功");
        } catch (IllegalArgumentException ex) {
            model.addAttribute("passwordForm", ChangePasswordForm.empty());
            model.addAttribute("emailError", ex.getMessage());
            return "users/profile";
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") ChangePasswordForm form, BindingResult binding, Model model, Principal principal, RedirectAttributes ra) {
        User u = userService.findByUsername(principal.getName());
        model.addAttribute("user", u);
        model.addAttribute("username", principal.getName());
        if (binding.hasErrors()) {
            model.addAttribute("profileForm", new ProfileForm(u.getEmail()));
            model.addAttribute("passwordError", binding.getAllErrors().isEmpty() ? "表单校验失败" : binding.getAllErrors().get(0).getDefaultMessage());
            return "users/profile";
        }
        try {
            userService.changePassword(principal.getName(), form);
            ra.addFlashAttribute("flashOk", "密码修改成功");
        } catch (IllegalArgumentException ex) {
            model.addAttribute("profileForm", new ProfileForm(u.getEmail()));
            model.addAttribute("passwordError", ex.getMessage());
            return "users/profile";
        }
        return "redirect:/profile";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    public String userList(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(defaultValue = "1") long page,
            Model model,
            Principal principal
    ) {
        UserQuery q = new UserQuery(username, email, enabled);
        IPage<User> result = userService.search(q, page, 10);
        model.addAttribute("q", q);
        model.addAttribute("page", result);
        model.addAttribute("username", principal.getName());
        model.addAttribute("usernameRaw", Objects.toString(username, ""));
        model.addAttribute("emailRaw", Objects.toString(email, ""));
        model.addAttribute("enabledRaw", enabled);
        return "users/list";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/users/{id}/toggle")
    public String toggleUserEnabled(@PathVariable Long id, RedirectAttributes ra) {
        try {
            userService.toggleEnabled(id);
            ra.addFlashAttribute("flashOk", "用户状态更新成功");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashBad", ex.getMessage());
        }
        return "redirect:/admin/users";
    }
}
