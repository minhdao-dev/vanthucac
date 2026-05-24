INSERT INTO users (email, password_hash, full_name, status, created_at, updated_at)
VALUES ('admin@vanthucac.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhuG',
        'Admin',
        'ACTIVE',
        NOW(),
        NOW());

INSERT INTO user_roles (user_id, role_id)
VALUES ((SELECT id FROM users WHERE email = 'admin@vanthucac.com'),
        (SELECT id FROM roles WHERE name = 'ADMIN'));