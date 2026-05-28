SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE bids;
TRUNCATE TABLE notifications;
TRUNCATE TABLE audit_logs;
TRUNCATE TABLE outbox_events;
TRUNCATE TABLE wallet_transactions;
TRUNCATE TABLE platform_commissions;
TRUNCATE TABLE escrow_records;
TRUNCATE TABLE payments;
TRUNCATE TABLE order_items;
TRUNCATE TABLE orders;
TRUNCATE TABLE cart_items;
TRUNCATE TABLE carts;
TRUNCATE TABLE listing_images;
TRUNCATE TABLE book_listings;
TRUNCATE TABLE book_catalogs;
TRUNCATE TABLE seller_wallets;
TRUNCATE TABLE seller_profiles;
TRUNCATE TABLE user_roles;
TRUNCATE TABLE users;

SET FOREIGN_KEY_CHECKS = 1;