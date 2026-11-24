package com.ledgerguard.multitenancy;
import com.ledgerguard.security.JwtUtil;
import io.jsonwebtoken.Claims;
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
  private final JwtUtil jwtUtil;
  public TenantFilter(JwtUtil jwtUtil) { this.jwtUtil = jwtUtil; }
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;
    String path = req.getRequestURI();
    if (path.startsWith("/api/admin/tenants") || path.startsWith("/api/auth/") || path.startsWith("/api/real-auth/") || path.equals("/api/health")) {
      chain.doFilter(request, response);
      return;
    }
    String tenantId = req.getHeader("X-Tenant-ID");
    if (tenantId == null || tenantId.isBlank()) {
      String orgHeader = req.getHeader("X-Org-Id");
      if (orgHeader != null && !orgHeader.isBlank()) {
        tenantId = orgHeader.trim();
      } else {
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
          try {
            Claims c = jwtUtil.extractAllClaims(auth.substring(7));
            String tokenOrg = c.get("orgId", String.class);
            if ("orgA".equalsIgnoreCase(tokenOrg)) tenantId = "alpha";
            else if ("orgB".equalsIgnoreCase(tokenOrg)) tenantId = "beta";
            else tenantId = tokenOrg;
          } catch (Exception ignored) {}
        }
      }
    }
    if (tenantId == null || tenantId.isBlank()) {
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      res.getWriter().write("Missing X-Tenant-ID header");
      return;
    }
    try {
      log.debug("Setting tenant context to: {}", tenantId);
      TenantContext.setTenantId(tenantId.trim());
      chain.doFilter(request, response);
    } finally {
      log.info("Clearing tenant context for: {}", tenantId);
      TenantContext.clear();
    }
  }
}