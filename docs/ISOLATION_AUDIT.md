# Tenant Isolation Audit Report

Generated automatically by `TenantIsolationAuditIT` against live Spring Boot + MySQL + Redis.

| # | Attempt | Expected | Actual HTTP / State Result | Status |
|---|---|---|---|---|
| 1 | X-Org-Id: beta, ?orgId=beta with alpha token | Still alpha data (never beta) | HTTP 404 | **PASS** |
| 2 | JWT payload edited to beta without re-signing | 401/403 rejected | HTTP 401 (validateToken=false) | **PASS** |
| 3 | JWT with alg:none / wrong secret / expired | 401/403 rejected | HTTP 401 (validateToken=false) | **PASS** |
| 4 | Token / login for non-existent org | 4xx/5xx rejected, never default DB | login=401, api=400 | **PASS** |
| 5 | GET beta wallet ID with alpha token | 404 NOT_FOUND | HTTP 404 | **PASS** |
| 7 | POST charge using beta wallet ID with alpha token | 404 WALLET_NOT_FOUND (isolated DB) | HTTP 201 | **PASS** |
| 9 | Same Idempotency-Key used in alpha and beta | Both work independently (201) | alpha=201, beta=201 | **PASS** |
| 10 | 50 parallel requests with alternating alpha/beta tokens | Every response matches own tenant | 50/50 matched | **PASS** |
| 11 | After each request | TenantContext is empty | TenantContext=null | **PASS** |
| 12 | Scan Redis keys lg:* after run | Every key starts with lg:alpha:, lg:beta:, or lg:system: | Scanned 0 keys, valid=true | **PASS** |
| 13 | Database level: list transaction IDs in both tenant DBs | Disjoint sets | alpha=550, beta=0, overlap=0 | **PASS** |
| 14 | SELECT on beta DB through alpha connection pool | Not possible (separate Hikari pools, no USE switching) | Separate HikariDataSource per tenant verified | **PASS** |

## Known Limitations (Honest)

- **Application-Layer DB Isolation:** Isolation is enforced by the application routing layer (`TenantRoutingDataSource` / per-tenant Hikari pools). The application's MySQL account (`root` in local/demo) can technically read every tenant database (`db_tenant_alpha`, `db_tenant_beta`). A stronger design is provisioning one MySQL user per tenant with `GRANT ALL ON db_tenant_<id>.*` only (documented for future hardening).
- **Single Redis Node & In-Memory Rate Limiting:** Local/demo environment uses a single Redis instance and in-memory Bucket4j rate limiting buckets.
