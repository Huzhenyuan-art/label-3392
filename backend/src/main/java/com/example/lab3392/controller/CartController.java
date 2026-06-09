package com.example.lab3392.controller;

import com.example.lab3392.dto.CartAddForm;
import com.example.lab3392.dto.CartUpdateQuantityForm;
import com.example.lab3392.entity.CartItem;
import com.example.lab3392.service.CartService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart")
    public String cartList(Model model, Principal principal) {
        List<CartItem> items = cartService.getCurrentUserCart();
        BigDecimal total = cartService.calculateTotal(items);
        model.addAttribute("items", items);
        model.addAttribute("total", total);
        model.addAttribute("username", principal != null ? principal.getName() : "");
        model.addAttribute("quantityForm", new CartUpdateQuantityForm(1));
        return "cart/list";
    }

    @PostMapping("/cart/add")
    public String addToCart(@Valid @ModelAttribute("form") CartAddForm form,
            BindingResult binding, RedirectAttributes ra) {
        if (binding.hasErrors()) {
            ra.addFlashAttribute("flashError",
                    binding.getAllErrors().isEmpty() ? "参数错误" : binding.getAllErrors().get(0).getDefaultMessage());
            if (form.productId() != null) {
                return "redirect:/products/" + form.productId();
            }
            return "redirect:/products";
        }
        try {
            cartService.addToCart(form);
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/products/" + form.productId();
        }
        ra.addFlashAttribute("flashOk", "已加入购物车");
        return "redirect:/cart";
    }

    @PostMapping("/cart/{id}/update")
    public String updateQuantity(@PathVariable Long id,
            @Valid @ModelAttribute("quantityForm") CartUpdateQuantityForm form,
            BindingResult binding, RedirectAttributes ra) {
        if (binding.hasErrors()) {
            ra.addFlashAttribute("flashError",
                    binding.getAllErrors().isEmpty() ? "参数错误" : binding.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/cart";
        }
        try {
            cartService.updateQuantity(id, form.quantity());
        } catch (IllegalArgumentException | SecurityException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/cart";
        }
        ra.addFlashAttribute("flashOk", "数量已更新");
        return "redirect:/cart";
    }

    @PostMapping("/cart/{id}/delete")
    public String deleteItem(@PathVariable Long id, RedirectAttributes ra) {
        try {
            cartService.removeItem(id);
        } catch (IllegalArgumentException | SecurityException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/cart";
        }
        ra.addFlashAttribute("flashOk", "已从购物车移除");
        return "redirect:/cart";
    }

    @GetMapping("/cart/checkout")
    public String checkout(Model model, Principal principal) {
        List<CartItem> items = cartService.getCurrentUserCart();
        List<String> errors = cartService.validateCartForCheckout(items);
        BigDecimal total = cartService.calculateTotal(items);
        int totalQuantity = items.stream().mapToInt(CartItem::getQuantity).sum();

        model.addAttribute("items", items);
        model.addAttribute("total", total);
        model.addAttribute("errors", errors);
        model.addAttribute("canCheckout", errors.isEmpty());
        model.addAttribute("totalQuantity", totalQuantity);
        model.addAttribute("username", principal != null ? principal.getName() : "");
        return "cart/checkout";
    }
}
