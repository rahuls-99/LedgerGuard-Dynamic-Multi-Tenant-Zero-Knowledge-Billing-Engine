package com.ledgerguard.service;

import com.ledgerguard.dto.AuthUserPrincipal;
import com.ledgerguard.dto.RealLoginRequest;
import com.ledgerguard.dto.RealLoginResponse;
import com.ledgerguard.model.TenantUser;
import com.ledgerguard.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RealAuthService {

    private final TenantConnectionProvider tenantProvider;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public RealAuthService(TenantConnectionProvider tenantProvider, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.tenantProvider = tenantProvider;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public Optional<RealLoginResponse> login(RealLoginRequest request) {
        Optional<TenantUser> userOpt = tenantProvider.findUserInTenantDb(request.getOrgId(), request.getEmail());

        if (userOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOpt.get().getPasswordHash())) {
            return Optional.empty();
        }

        TenantUser user = userOpt.get();
        String token = jwtUtil.signToken(user.getId(), user.getOrgId(), user.getRole());
        AuthUserPrincipal principal = new AuthUserPrincipal(user.getId(), user.getOrgId(), user.getRole());

        return Optional.of(new RealLoginResponse(true, token, principal));
    }
}