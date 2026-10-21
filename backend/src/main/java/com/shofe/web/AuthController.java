package com.shofe.web;

import com.shofe.dto.AuthDtos;
import com.shofe.service.AuthService;
import com.shofe.service.CurrentUserService;
import com.shofe.service.OtpService;
import com.shofe.service.PhoneNumbers;
import com.shofe.service.UserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;
    private final CurrentUserService currentUserService;
    private final UserMapper userMapper;

    @GetMapping("/countries")
    public List<PhoneNumbers.Country> countries() {
        return PhoneNumbers.COUNTRIES;
    }

    @PostMapping("/otp/request")
    public AuthDtos.OtpSentResponse requestOtp(@Valid @RequestBody AuthDtos.OtpRequest request) {
        return otpService.request(request);
    }

    @PostMapping("/otp/verify")
    public AuthDtos.AuthResponse verifyOtp(@Valid @RequestBody AuthDtos.OtpVerifyRequest request) {
        return otpService.verify(request);
    }

    @PostMapping("/register")
    public AuthDtos.AuthResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AuthDtos.UserResponse me() {
        return userMapper.toUser(currentUserService.require());
    }
}
