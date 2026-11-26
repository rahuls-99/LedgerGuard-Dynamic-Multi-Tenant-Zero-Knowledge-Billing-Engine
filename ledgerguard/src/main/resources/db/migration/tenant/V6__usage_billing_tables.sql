CREATE TABLE IF NOT EXISTS usage_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    resource_code VARCHAR(64) NOT NULL,
    resource_name VARCHAR(128) NOT NULL,
    unit VARCHAR(32) NOT NULL,
    quantity DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    unit_price DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
    recorded_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_usage_tenant_date_res (tenant_id, recorded_date, resource_code)
);

CREATE TABLE IF NOT EXISTS billing_invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_number VARCHAR(64) NOT NULL UNIQUE,
    tenant_id VARCHAR(64) NOT NULL,
    billing_period_start DATE NOT NULL,
    billing_period_end DATE NOT NULL,
    currency VARCHAR(8) NOT NULL DEFAULT 'INR',
    subtotal_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    issued_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_invoice_tenant_period (tenant_id, billing_period_start, billing_period_end, status)
);

CREATE TABLE IF NOT EXISTS billing_invoice_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    resource_code VARCHAR(64) NOT NULL,
    description VARCHAR(255) NOT NULL,
    quantity DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    unit_price DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    line_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_invoice_items_invoice
        FOREIGN KEY (invoice_id) REFERENCES billing_invoices(id)
        ON DELETE CASCADE
);