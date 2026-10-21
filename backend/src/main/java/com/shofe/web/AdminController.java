package com.shofe.web;

import com.shofe.dto.AuthDtos;
import com.shofe.dto.MiscDtos;
import com.shofe.dto.RideDtos;
import com.shofe.service.AdminService;
import com.shofe.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final DriverService driverService;

    @GetMapping("/stats")
    public MiscDtos.AdminStats stats() {
        return adminService.stats();
    }

    @GetMapping("/analytics")
    public MiscDtos.AdminAnalytics analytics() {
        return adminService.analytics();
    }

    @GetMapping("/sos")
    public List<MiscDtos.SosEventView> sos() {
        return adminService.sosFeed();
    }

    @GetMapping("/users")
    public List<AuthDtos.UserResponse> users() {
        return adminService.users();
    }

    @GetMapping("/rides")
    public List<RideDtos.RideResponse> rides() {
        return adminService.rides();
    }

    @PatchMapping("/drivers/{userId}/verification")
    public AuthDtos.DriverSummary verify(@PathVariable UUID userId, @Valid @RequestBody MiscDtos.VerificationRequest request) {
        return driverService.verify(userId, request);
    }
}
