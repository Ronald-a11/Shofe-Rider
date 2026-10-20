package com.shofe.service;

import com.shofe.domain.entity.DriverProfile;
import com.shofe.domain.entity.User;
import com.shofe.domain.enums.Role;
import com.shofe.dto.AuthDtos;
import com.shofe.exception.ApiException;
import com.shofe.repo.UserRepository;
import com.shofe.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            throw ApiException.badRequest("Admin accounts cannot be self-registered");
        }
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw ApiException.conflict("An account with that email already exists");
        }
        if (users.existsByPhone(request.phone().trim())) {
            throw ApiException.conflict("An account with that phone already exists");
        }
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPhone(request.phone().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        if (request.role() == Role.DRIVER) {
            DriverProfile profile = new DriverProfile();
            profile.setUser(user);
            user.setDriverProfile(profile);
        }
        users.save(user);
        return new AuthDtos.AuthResponse(jwtService.issue(user), userMapper.toUser(user));
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BadCredentialsException("bad"));
        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("bad");
        }
        return new AuthDtos.AuthResponse(jwtService.issue(user), userMapper.toUser(user));
    }
}
