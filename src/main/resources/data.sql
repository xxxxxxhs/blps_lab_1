INSERT INTO app_users (username, full_name, phone, active) VALUES
('client1', 'Клиент 1', '79991111111', true),
('client2', 'Клиент 2', '79991111112', true),
('client3', 'Клиент 3', '79991111113', true),
('client4', 'Клиент 4', '79991111114', true),
('client5', 'Клиент 5', '79991111115', true)
ON CONFLICT (username) DO NOTHING;

INSERT INTO couriers (full_name, phone, city, login) VALUES
('Курьер 1', '79992222221', 'Москва', 'courier1'),
('Курьер 2', '79992222222', 'Москва', 'courier2'),
('Курьер 3', '79992222223', 'Москва', 'courier3'),
('Курьер 4', '79992222224', 'Москва', 'courier4'),
('Курьер 5', '79992222225', 'Москва', 'courier5')
ON CONFLICT (login) DO NOTHING;

INSERT INTO restaurants (name, city, address, login) VALUES
('Ресторан 1', 'Москва', 'ул. Примерная, 1', 'restaurant1'),
('Ресторан 2', 'Москва', 'ул. Примерная, 2', 'restaurant2'),
('Ресторан 3', 'Москва', 'ул. Примерная, 3', 'restaurant3'),
('Ресторан 4', 'Москва', 'ул. Примерная, 4', 'restaurant4'),
('Ресторан 5', 'Москва', 'ул. Примерная, 5', 'restaurant5')
ON CONFLICT (login) DO NOTHING;
