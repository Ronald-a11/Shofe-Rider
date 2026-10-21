package com.shofe.web;

import com.shofe.dto.AuthDtos;
import com.shofe.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PutMapping("/me")
    public AuthDtos.UserResponse update(@Valid @RequestBody AuthDtos.ProfileUpdate request) {
        return accountService.updateProfile(request);
    }

    @PostMapping("/mode")
    public AuthDtos.AuthResponse switchMode(@Valid @RequestBody AuthDtos.ModeRequest request) {
        return accountService.switchMode(request);
    }

    @GetMapping("/contacts")
    public List<AuthDtos.ContactResponse> contacts() {
        return accountService.contacts();
    }

    @PostMapping("/contacts")
    public AuthDtos.ContactResponse addContact(@Valid @RequestBody AuthDtos.ContactRequest request) {
        return accountService.addContact(request);
    }

    @DeleteMapping("/contacts/{id}")
    @ResponseStatus(NO_CONTENT)
    public void deleteContact(@PathVariable UUID id) {
        accountService.deleteContact(id);
    }
}
