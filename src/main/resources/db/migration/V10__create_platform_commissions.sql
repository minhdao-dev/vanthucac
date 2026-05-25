CREATE TABLE platform_commissions
(
    id         BIGINT         NOT NULL AUTO_INCREMENT,
    order_id   BIGINT         NOT NULL,
    seller_id  BIGINT         NOT NULL,
    amount     DECIMAL(15, 2) NOT NULL,
    rate       DECIMAL(5, 4)  NOT NULL,
    created_at DATETIME(6)    NOT NULL,

    CONSTRAINT pk_platform_commissions PRIMARY KEY (id),
    CONSTRAINT uk_platform_commissions_order UNIQUE (order_id),
    CONSTRAINT fk_platform_commissions_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_platform_commissions_seller FOREIGN KEY (seller_id) REFERENCES seller_profiles (id)
);