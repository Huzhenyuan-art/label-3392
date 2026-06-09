package com.example.lab3392.controller;

import com.example.lab3392.dto.DashboardStats;
import com.example.lab3392.service.DashboardService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public String page(Model model, Principal principal) {
        model.addAttribute("username", principal != null ? principal.getName() : "");
        return "dashboard/index";
    }

    @GetMapping("/api/dashboard/stats")
    @ResponseBody
    public DashboardStats stats() {
        return dashboardService.getStats();
    }
}
