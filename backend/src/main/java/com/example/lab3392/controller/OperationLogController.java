package com.example.lab3392.controller;

import com.example.lab3392.dto.OperationLogQuery;
import com.example.lab3392.service.OperationLogService;
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OperationLogController {
    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/operation-logs")
    public String list(
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") long page,
            Model model,
            Principal principal
    ) {
        String opTypeRaw = trimToNull(operationType);
        String startRaw = trimToNull(startDate);
        String endRaw = trimToNull(endDate);

        String dateError = null;
        LocalDate startVal = null;
        LocalDate endVal = null;

        boolean startPresent = startRaw != null;
        boolean endPresent = endRaw != null;
        if (startPresent ^ endPresent) {
            dateError = "时间区间需同时填写开始日期和结束日期（或同时留空）";
        } else if (startPresent) {
            try {
                startVal = LocalDate.parse(startRaw);
                endVal = LocalDate.parse(endRaw);
                if (startVal.isAfter(endVal)) {
                    dateError = "时间区间不合法：开始日期不能晚于结束日期";
                }
            } catch (DateTimeParseException ex) {
                dateError = "日期格式不正确，请使用 YYYY-MM-DD 格式";
            }
        }

        OperationLogQuery q = new OperationLogQuery(opTypeRaw, dateError == null ? startVal : null, dateError == null ? endVal : null);
        model.addAttribute("q", q);
        long safePage = dateError == null ? page : 1;
        model.addAttribute("page", operationLogService.search(q, safePage, 10));
        model.addAttribute("username", principal != null ? principal.getName() : "");
        model.addAttribute("operationTypeRaw", Objects.toString(opTypeRaw, ""));
        model.addAttribute("startDateRaw", Objects.toString(startRaw, ""));
        model.addAttribute("endDateRaw", Objects.toString(endRaw, ""));
        model.addAttribute("dateError", dateError);
        return "operation-logs/list";
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
