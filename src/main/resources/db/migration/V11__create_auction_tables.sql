CREATE TABLE auction_sessions
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    title      VARCHAR(255) NOT NULL,
    start_time DATETIME(6)  NOT NULL,
    end_time   DATETIME(6)  NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    created_by BIGINT       NOT NULL,
    created_at DATETIME(6)  NOT NULL,

    CONSTRAINT pk_auction_sessions PRIMARY KEY (id),
    CONSTRAINT fk_auction_sessions_created_by FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE TABLE auction_items
(
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    session_id        BIGINT         NOT NULL,
    book_catalog_id   BIGINT         NOT NULL,
    starting_price    DECIMAL(15, 2) NOT NULL,
    current_price     DECIMAL(15, 2) NOT NULL,
    min_bid_increment DECIMAL(15, 2) NOT NULL,
    winner_id         BIGINT,
    status            VARCHAR(20)    NOT NULL DEFAULT 'WAITING',
    version           INT            NOT NULL DEFAULT 0,

    CONSTRAINT pk_auction_items PRIMARY KEY (id),
    CONSTRAINT fk_auction_items_session FOREIGN KEY (session_id) REFERENCES auction_sessions (id),
    CONSTRAINT fk_auction_items_book FOREIGN KEY (book_catalog_id) REFERENCES book_catalogs (id),
    CONSTRAINT fk_auction_items_winner FOREIGN KEY (winner_id) REFERENCES users (id)
);

CREATE TABLE bids
(
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    auction_item_id BIGINT         NOT NULL,
    user_id         BIGINT         NOT NULL,
    amount          DECIMAL(15, 2) NOT NULL,
    created_at      DATETIME(6)    NOT NULL,

    CONSTRAINT pk_bids PRIMARY KEY (id),
    CONSTRAINT fk_bids_auction_item FOREIGN KEY (auction_item_id) REFERENCES auction_items (id),
    CONSTRAINT fk_bids_user FOREIGN KEY (user_id) REFERENCES users (id)
);