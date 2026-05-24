CREATE TABLE orders
(
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    user_id         BIGINT         NOT NULL,
    parent_order_id BIGINT,
    seller_id       BIGINT,
    total_amount    DECIMAL(15, 2) NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    order_type      VARCHAR(10)    NOT NULL,
    shipping_address VARCHAR(500),
    created_at      DATETIME(6)    NOT NULL,
    updated_at      DATETIME(6)    NOT NULL,

    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_orders_parent FOREIGN KEY (parent_order_id) REFERENCES orders (id),
    CONSTRAINT fk_orders_seller FOREIGN KEY (seller_id) REFERENCES seller_profiles (id)
);

CREATE TABLE order_items
(
    id         BIGINT         NOT NULL AUTO_INCREMENT,
    order_id   BIGINT         NOT NULL,
    listing_id BIGINT         NOT NULL,
    quantity   INT            NOT NULL,
    unit_price DECIMAL(15, 2) NOT NULL,

    CONSTRAINT pk_order_items PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_items_listing FOREIGN KEY (listing_id) REFERENCES book_listings (id)
);

CREATE TABLE payments
(
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    order_id       BIGINT         NOT NULL,
    amount         DECIMAL(15, 2) NOT NULL,
    status         VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50)    NOT NULL,
    created_at     DATETIME(6)    NOT NULL,

    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE TABLE escrow_records
(
    id         BIGINT         NOT NULL AUTO_INCREMENT,
    order_id   BIGINT         NOT NULL,
    amount     DECIMAL(15, 2) NOT NULL,
    status     VARCHAR(20)    NOT NULL DEFAULT 'HOLDING',
    created_at DATETIME(6)    NOT NULL,
    updated_at DATETIME(6)    NOT NULL,

    CONSTRAINT pk_escrow_records PRIMARY KEY (id),
    CONSTRAINT uk_escrow_records_order UNIQUE (order_id),
    CONSTRAINT fk_escrow_records_order FOREIGN KEY (order_id) REFERENCES orders (id)
);