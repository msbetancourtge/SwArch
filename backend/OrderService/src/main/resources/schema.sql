CREATE TABLE IF NOT EXISTS orders (
    id            SERIAL PRIMARY KEY,
    restaurant_id BIGINT       NOT NULL,
    table_number  INT          NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    notes         TEXT,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- One row per ordered unit so per-unit notes (e.g. "sin lechuga") can differ
-- between two units of the same item. The frontend groups visually by
-- (item_name, notes) when showing the kitchen view.
CREATE TABLE IF NOT EXISTS order_items (
    id         SERIAL PRIMARY KEY,
    order_id   BIGINT       NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    item_name  VARCHAR(200) NOT NULL,
    notes      TEXT
);
