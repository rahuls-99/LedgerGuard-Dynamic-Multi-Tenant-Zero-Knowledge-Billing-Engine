package com.ledgerguard.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tenants")
@Data
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", unique = true, nullable = false)
    private String tenantId; // जैसे: "tenant_alpha"

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "db_name", unique = true, nullable = false)
    private String dbName; // जैसे: "db_tenant_alpha"

    @Column(name = "db_url", nullable = false)
    private String dbUrl;

    @Column(name = "db_username", nullable = false)
    private String dbUsername;

    @Column(name = "db_password", nullable = false)
    private String dbPassword;

    private String status = "ACTIVE";
}