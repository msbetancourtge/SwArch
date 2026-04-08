CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    customer_name VARCHAR(200) NOT NULL,
    restaurant_id BIGINT NOT NULL,
    restaurant_name VARCHAR(200) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'Preparing',
    channel VARCHAR(50) NOT NULL DEFAULT 'In-person',
    notes TEXT,
    eta VARCHAR(50),
    total DECIMAL(10,2) NOT NULL DEFAULT 0,
    table_id BIGINT,
    waiter_id BIGINT,
    tip_amount DECIMAL(10,2) DEFAULT 0,
    waiter_comment TEXT,
    preparation_minutes INTEGER,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS order_items (
    id SERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    menu_item_id VARCHAR(100) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS waiter_calls (
    id SERIAL PRIMARY KEY,
    order_id BIGINT REFERENCES orders(id),
    table_id BIGINT,
    restaurant_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    message TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    resolved_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_restaurant_id ON orders(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_waiter_id ON orders(waiter_id);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_waiter_calls_restaurant_id ON waiter_calls(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_waiter_calls_status ON waiter_calls(restaurant_id, status);
