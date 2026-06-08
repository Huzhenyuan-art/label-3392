package com.example.lab3392.controller;

import com.example.lab3392.dto.NotificationQuery;
import com.example.lab3392.entity.Notification;
import com.example.lab3392.entity.User;
import com.example.lab3392.service.NotificationService;
import com.example.lab3392.service.UserService;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class NotificationController {
    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    private User getCurrentUser(Principal principal) {
        if (principal == null) return null;
        return userService.findByUsername(principal.getName());
    }

    @GetMapping("/notifications")
    public String list(
            @RequestParam(required = false, defaultValue = "false") Boolean unreadOnly,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            Model model,
            Principal principal
    ) {
        User user = getCurrentUser(principal);
        if (user == null) return "redirect:/login";

        long safePageSize = (pageSize == 10 || pageSize == 20 || pageSize == 50) ? pageSize : 10;
        NotificationQuery q = new NotificationQuery(unreadOnly, type);

        model.addAttribute("page", notificationService.search(user.getId(), q, page, safePageSize));
        model.addAttribute("unreadCount", notificationService.countUnread(user.getId()));
        model.addAttribute("q", q);
        model.addAttribute("unreadOnly", unreadOnly);
        model.addAttribute("type", type);
        model.addAttribute("pageSize", safePageSize);
        model.addAttribute("username", principal != null ? principal.getName() : "");
        return "notifications/list";
    }

    @GetMapping("/api/notifications/unread-count")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getUnreadCount(Principal principal) {
        User user = getCurrentUser(principal);
        if (user == null) {
            return ResponseEntity.ok(Map.of("count", 0L));
        }
        long count = notificationService.countUnread(user.getId());
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/notifications/{id}/read")
    public String markAsRead(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") Boolean unreadOnly,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            Principal principal,
            RedirectAttributes ra
    ) {
        User user = getCurrentUser(principal);
        if (user == null) return "redirect:/login";

        try {
            notificationService.markAsRead(user.getId(), id);
            ra.addFlashAttribute("flashOk", "已标记为已读");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("flashBad", e.getMessage());
        }

        StringBuilder redirectUrl = new StringBuilder("redirect:/notifications?");
        redirectUrl.append("page=").append(page);
        redirectUrl.append("&pageSize=").append(pageSize);
        if (unreadOnly) {
            redirectUrl.append("&unreadOnly=true");
        }
        return redirectUrl.toString();
    }

    @PostMapping("/notifications/mark-all-read")
    public String markAllAsRead(Principal principal, RedirectAttributes ra) {
        User user = getCurrentUser(principal);
        if (user == null) return "redirect:/login";

        notificationService.markAllAsRead(user.getId());
        ra.addFlashAttribute("flashOk", "所有通知已标记为已读");
        return "redirect:/notifications";
    }

    @PostMapping("/api/notifications/{id}/read")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> markAsReadApi(
            @PathVariable Long id,
            Principal principal
    ) {
        User user = getCurrentUser(principal);
        Map<String, Boolean> response = new HashMap<>();
        if (user == null) {
            response.put("success", false);
            return ResponseEntity.ok(response);
        }

        try {
            notificationService.markAsRead(user.getId(), id);
            response.put("success", true);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
        }
        return ResponseEntity.ok(response);
    }
}
