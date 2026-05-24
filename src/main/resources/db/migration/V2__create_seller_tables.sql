CREATE TABLE seller_profiles
(
    id          INT          NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    shop_name   VARCHAR(255) NOT NULL,
    description TEXT,
    status      VARCHAR(20)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_seller_profiles_user_id (user_id),
    CONSTRAINT fk_seller_profiles_user_id
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE seller_wallets
(
    id         INT            NOT NULL AUTO_INCREMENT,
    seller_id  INT            NOT NULL,
    balance    DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    updated_at DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_seller_wallets_seller_id (seller_id),
    CONSTRAINT fk_seller_wallets_seller_id
        FOREIGN KEY (seller_id) REFERENCES seller_profiles (id)
);