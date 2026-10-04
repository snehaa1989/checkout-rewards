package com.example.checkout_rewards.controller;

import com.example.checkout_rewards.dto.AdminReportResponse;
import com.example.checkout_rewards.dto.CouponResponse;
import com.example.checkout_rewards.service.AdminReportService;
import com.example.checkout_rewards.service.CouponService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final CouponService couponService;
    private final AdminReportService adminReportService;

    public AdminController(
            CouponService couponService,
            AdminReportService adminReportService
    ) {
        this.couponService = couponService;
        this.adminReportService = adminReportService;
    }

    @PostMapping("/coupons/generate")
    public CouponResponse generateCoupon() {
        return couponService.generateCoupon();
    }

    @GetMapping("/report")
    public AdminReportResponse getReport() {
        return adminReportService.generateReport();
    }
}