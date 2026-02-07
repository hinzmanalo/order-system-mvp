-- Seed Users
-- Note: These are BCrypt hashes for testing purposes only
-- admin@orderhub.com / admin123
-- user@orderhub.com / user123
INSERT INTO users (id, email, password_hash, first_name, last_name, role, created_at, updated_at)
VALUES 
    ('550e8400-e29b-41d4-a716-446655440001', 'admin@orderhub.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Admin', 'User', 'ADMIN', NOW(), NOW()),
    ('550e8400-e29b-41d4-a716-446655440002', 'user@orderhub.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Test', 'User', 'USER', NOW(), NOW());

-- Seed Products
INSERT INTO products (id, name, description, price, sku, active, version, created_at, updated_at)
VALUES 
    ('650e8400-e29b-41d4-a716-446655440001', 'Smartphone X', 'Latest flagship smartphone with advanced features', 299.99, 'PHONE-001', true, 0, NOW(), NOW()),
    ('650e8400-e29b-41d4-a716-446655440002', 'Laptop Pro', 'High-performance laptop for professionals', 999.99, 'LAPTOP-001', true, 0, NOW(), NOW()),
    ('650e8400-e29b-41d4-a716-446655440003', 'Wireless Earbuds', 'Premium wireless earbuds with noise cancellation', 79.99, 'AUDIO-001', true, 0, NOW(), NOW()),
    ('650e8400-e29b-41d4-a716-446655440004', 'USB-C Cable', 'Durable USB-C charging cable', 12.99, 'CABLE-001', true, 0, NOW(), NOW()),
    ('650e8400-e29b-41d4-a716-446655440005', 'Phone Case', 'Protective phone case with stylish design', 19.99, 'CASE-001', true, 0, NOW(), NOW());

-- Seed Inventory
INSERT INTO inventory (id, product_id, quantity, version, updated_at)
VALUES 
    ('750e8400-e29b-41d4-a716-446655440001', '650e8400-e29b-41d4-a716-446655440001', 50, 0, NOW()),
    ('750e8400-e29b-41d4-a716-446655440002', '650e8400-e29b-41d4-a716-446655440002', 75, 0, NOW()),
    ('750e8400-e29b-41d4-a716-446655440003', '650e8400-e29b-41d4-a716-446655440003', 100, 0, NOW()),
    ('750e8400-e29b-41d4-a716-446655440004', '650e8400-e29b-41d4-a716-446655440004', 200, 0, NOW()),
    ('750e8400-e29b-41d4-a716-446655440005', '650e8400-e29b-41d4-a716-446655440005', 150, 0, NOW());
