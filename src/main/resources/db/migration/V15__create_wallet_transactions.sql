CREATE TABLE wallet_transactions
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_id        BIGINT         NOT NULL,
    transaction_type VARCHAR(30)    NOT NULL,
    amount           DECIMAL(15, 2) NOT NULL,
    balance_before   DECIMAL(15, 2) NOT NULL,
    balance_after    DECIMAL(15, 2) NOT NULL,
    reference_type   VARCHAR(50)    NOT NULL,
    reference_id     BIGINT         NOT NULL,
    description      VARCHAR(500),
    created_at       DATETIME(6) NOT NULL,

    CONSTRAINT fk_wallet_transactions_wallet
        FOREIGN KEY (wallet_id) REFERENCES seller_wallets (id),

    CONSTRAINT uk_wallet_transaction_reference
        UNIQUE (wallet_id, transaction_type, reference_type, reference_id)
);

CREATE INDEX idx_wallet_transactions_wallet_id
    ON wallet_transactions (wallet_id);

CREATE INDEX idx_wallet_transactions_reference
    ON wallet_transactions (reference_type, reference_id);