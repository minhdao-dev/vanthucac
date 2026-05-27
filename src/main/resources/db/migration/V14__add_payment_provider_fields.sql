ALTER TABLE payments
    ADD COLUMN provider_payment_id VARCHAR(100),
    ADD COLUMN checkout_url VARCHAR(500),
    ADD COLUMN paid_at DATETIME(6),
    ADD COLUMN failed_at DATETIME(6),
    ADD COLUMN updated_at DATETIME(6);

CREATE INDEX idx_payments_provider_payment_id ON payments (provider_payment_id);
CREATE INDEX idx_payments_status ON payments (status);