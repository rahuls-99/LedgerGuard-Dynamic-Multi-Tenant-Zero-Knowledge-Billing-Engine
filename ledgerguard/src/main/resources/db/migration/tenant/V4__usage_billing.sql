CREATE TABLE IF NOT EXISTS resources (
    code VARCHAR(32) NOT NULL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    unit VARCHAR(16) NOT NULL,
    unit_price DECIMAL(19,4) NOT NULL,
    currency CHAR(3) NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT chk_res_price CHECK (unit_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS usage_records (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resource_code VARCHAR(32) NOT NULL,
    quantity DECIMAL(19,4) NOT NULL,
    unit_price DECIMAL(19,4) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    occurred_at TIMESTAMP(3) NOT NULL,
    source VARCHAR(16) NOT NULL DEFAULT 'SYSTEM',
    KEY idx_usage_resource_time (resource_code, occurred_at),
    KEY idx_usage_time (occurred_at),
    CONSTRAINT chk_usage_qty CHECK (quantity > 0),
    CONSTRAINT fk_usage_resource FOREIGN KEY (resource_code) REFERENCES resources(code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS usage_daily (
    usage_date DATE NOT NULL,
    resource_code VARCHAR(32) NOT NULL,
    quantity DECIMAL(19,4) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    record_count INT NOT NULL,
    PRIMARY KEY (usage_date, resource_code),
    KEY idx_daily_resource_date (resource_code, usage_date),
    CONSTRAINT fk_daily_resource FOREIGN KEY (resource_code) REFERENCES resources(code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_tx_created_status_type_amount
    ON transactions (created_at, status, type, amount);

INSERT IGNORE INTO resources (code, name, unit, unit_price, currency) VALUES
    ('API_CALLS', 'API calls', 'calls', 0.0004, 'INR'),
    ('STORAGE_GB', 'Storage', 'GB-day', 0.0500, 'INR'),
    ('COMPUTE_HOURS', 'Compute', 'hours', 1.5000, 'INR'),
    ('SEATS', 'Seats', 'seat-day', 0.5000, 'INR');