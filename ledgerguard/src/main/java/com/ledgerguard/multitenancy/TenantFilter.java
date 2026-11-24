package com.ledgerguard.multitenancy;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TenantFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getRequestURI();

        // नया Tenant बनाने वाली API को बिना X-Tenant-ID के जाने दें
        if (path.startsWith("/api/admin/tenants")
                || path.startsWith("/api/auth/")
                || path.startsWith("/api/real-auth/")
                || path.equals("/api/health")) {
            chain.doFilter(request, response);
            return;
        }

        String tenantId = req.getHeader("X-Tenant-ID");
        if (tenantId == null || tenantId.isBlank()) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("Missing X-Tenant-ID header");
            return;
        }

        try {
            log.debug("Setting tenant context to: {}", tenantId);
            TenantContext.setTenantId(tenantId);
            chain.doFilter(request, response);
        } finally {
            log.info("Clearing tenant context for: {}", tenantId);
            TenantContext.clear();
        }
    }
}