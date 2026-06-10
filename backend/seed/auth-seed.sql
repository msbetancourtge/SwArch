
-- contraseña: password123
INSERT INTO users (id, name, username, email, password_hash, role, approval_status)
VALUES (1, 'manager1', 'manager1', 'juan@restaurant.com', '$2a$10$GKOvJHmSVH8Wn2j8Cx5RAOEa.hBEBYPceZGh4YLrxaZ79XsjoRH2C', 'RESTAURANT_MANAGER', 'APPROVED')
ON CONFLICT DO NOTHING;

SELECT setval('users_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM users), 1));
