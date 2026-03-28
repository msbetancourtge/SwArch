CREATE TABLE IF NOT EXISTS orders (
    id            SERIAL PRIMARY KEY,
    restaurant_id BIGINT       NOT NULL,
    table_number  INT          NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    notes         TEXT,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_items (
    id         SERIAL PRIMARY KEY,
    order_id   BIGINT       NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    item_name  VARCHAR(200) NOT NULL,
    quantity   INT          NOT NULL DEFAULT 1,
    notes      TEXT
);
