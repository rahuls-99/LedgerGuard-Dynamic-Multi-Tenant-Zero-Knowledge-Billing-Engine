USE db_tenant_alpha;
DELETE FROM users WHERE email = 'ravi@alpha.com';
INSERT INTO users (name, email, password, role) VALUES ('Ravi', 'ravi@alpha.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN');
SELECT id, email, LEFT(password, 15) FROM users;
