package com.shofe.service;

import com.shofe.domain.entity.User;
import com.shofe.exception.ApiException;
import com.shofe.repo.UserRepository;
import com.shofe.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository users;

    public User require() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser principal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return users.findById(principal.getId()).orElseThrow(() -> ApiException.notFound("User not found"));
    }
}
