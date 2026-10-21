package com.shofe.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shofe.dto.AuthDtos;
import com.shofe.dto.MiscDtos;
import com.shofe.exception.ApiException;
import com.shofe.service.CurrentUserService;
import com.shofe.service.DriverService;
import com.shofe.service.SafetyMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;
    private final ObjectMapper objectMapper;
    private final CurrentUserService currentUserService;
    private final SafetyMonitorService safetyMonitor;

    @PostMapping(value = "/profile", consumes = "multipart/form-data")
    public AuthDtos.UserResponse profile(
            @RequestPart("profile") String profileJson,
            @RequestPart(value = "license", required = false) MultipartFile license,
            @RequestPart(value = "registration", required = false) MultipartFile registration,
            @RequestPart(value = "insurance", required = false) MultipartFile insurance) {
        try {
            MiscDtos.DriverProfileRequest request = objectMapper.readValue(profileJson, MiscDtos.DriverProfileRequest.class);
            return driverService.saveProfile(request, license, registration, insurance);
        } catch (IOException ex) {
            throw ApiException.badRequest("Profile details could not be read");
        }
    }

    @PostMapping("/online")
    public AuthDtos.UserResponse online(@jakarta.validation.Valid @RequestBody MiscDtos.OnlineRequest request) {
        return driverService.goOnline(request);
    }

    @PostMapping("/offline")
    public AuthDtos.UserResponse offline() {
        return driverService.goOffline();
    }

    @GetMapping("/fatigue")
    public Map<String, Object> fatigue() {
        var user = currentUserService.require();
        return safetyMonitor.fatigueStatus(user.getDriverProfile());
    }
}
