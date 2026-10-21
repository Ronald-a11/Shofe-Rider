package com.shofe.security;

import com.shofe.domain.entity.User;
import com.shofe.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = users.findByPhone(username)
                .or(() -> users.findByEmailIgnoreCase(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        String principal = user.getPhone() != null ? user.getPhone() : user.getEmail();
        String secret = user.getPasswordHash() == null ? "" : user.getPasswordHash();
        return new AuthUser(user.getId(), principal, secret, user.getRole(), user.isEnabled());
    }
}
