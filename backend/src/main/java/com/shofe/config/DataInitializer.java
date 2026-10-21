package com.shofe.config;

import com.shofe.domain.entity.User;
import com.shofe.domain.enums.ActiveMode;
import com.shofe.domain.enums.Role;
import com.shofe.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Optional bootstrap admin only. No demo rider/driver accounts are seeded.
 * Set SHOFE_BOOTSTRAP_ADMIN_EMAIL and SHOFE_BOOTSTRAP_ADMIN_PASSWORD to create the first admin.
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    @Value("${shofe.bootstrap.admin-email:}")
    private String adminEmail;

    @Value("${shofe.bootstrap.admin-password:}")
    private String adminPassword;

    @Value("${shofe.bootstrap.admin-phone:+263771000001}")
    private String adminPhone;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)) {
            return;
        }
        if (users.existsByEmailIgnoreCase(adminEmail.trim())) {
            return;
        }
        User user = new User();
        user.setFullName("Shofe Admin");
        user.setEmail(adminEmail.trim().toLowerCase());
        user.setPhone(adminPhone.trim());
        user.setCountryIso("ZW");
        user.setPasswordHash(passwordEncoder.encode(adminPassword));
        user.setRole(Role.ADMIN);
        user.setActiveMode(ActiveMode.PASSENGER);
        user.setPhoneVerified(true);
        users.save(user);
        log.info("Bootstrap admin created");
    }
}
