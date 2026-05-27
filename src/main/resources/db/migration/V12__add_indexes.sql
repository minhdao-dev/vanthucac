CREATE INDEX idx_book_listings_status ON book_listings (status);
CREATE INDEX idx_book_listings_seller_id ON book_listings (seller_id);
CREATE INDEX idx_book_listings_book_catalog ON book_listings (book_catalog_id);

CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_seller_id ON orders (seller_id);
CREATE INDEX idx_orders_parent_id ON orders (parent_order_id);

CREATE INDEX idx_bids_auction_item_id ON bids (auction_item_id);
CREATE INDEX idx_bids_user_id ON bids (user_id);

CREATE INDEX idx_auction_sessions_status ON auction_sessions (status);
CREATE INDEX idx_auction_sessions_start_time ON auction_sessions (start_time);
CREATE INDEX idx_auction_sessions_end_time ON auction_sessions (end_time);

CREATE INDEX idx_auction_items_session_id ON auction_items (session_id);

CREATE INDEX idx_carts_user_id ON carts (user_id);
CREATE INDEX idx_listing_images_listing ON listing_images (listing_id);
CREATE INDEX idx_escrow_records_order_id ON escrow_records (order_id);