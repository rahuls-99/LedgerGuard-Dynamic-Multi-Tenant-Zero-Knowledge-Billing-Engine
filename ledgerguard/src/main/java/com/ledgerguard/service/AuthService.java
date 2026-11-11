package com.ledgerguard.service;

import com.ledgerguard.dto.LoginRequest;
import com.ledgerguard.dto.LoginResponse;
import com.ledgerguard.dto.UserDto;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    public Optional<LoginResponse> mockAuthenticate(LoginRequest request) {
        if ("admin@orga.com".equals(request.getEmail()) && "123456".equals(request.getPassword())) {
            UserDto user = new UserDto("u1", "Admin", "admin", "orgA", "Org A");
            return Optional.of(new LoginResponse(true, "mock-jwt-token", user));
        }
        return Optional.empty();
    }
}