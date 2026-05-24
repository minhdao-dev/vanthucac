CREATE TABLE book_listings
(
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    book_catalog_id BIGINT         NOT NULL,
    seller_id       BIGINT,
    price           DECIMAL(15, 2) NOT NULL,
    `condition`     VARCHAR(20)    NOT NULL,
    stock           INT            NOT NULL,
    listing_type    VARCHAR(10)    NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'PENDING_REVIEW',
    version         INT            NOT NULL DEFAULT 0,
    created_at      DATETIME(6)    NOT NULL,
    updated_at      DATETIME(6)    NOT NULL,

    CONSTRAINT pk_book_listings PRIMARY KEY (id),
    CONSTRAINT fk_book_listings_catalog FOREIGN KEY (book_catalog_id) REFERENCES book_catalogs (id),
    CONSTRAINT fk_book_listings_seller FOREIGN KEY (seller_id) REFERENCES seller_profiles (id)
);

CREATE TABLE listing_images
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    listing_id BIGINT       NOT NULL,
    image_url  VARCHAR(500) NOT NULL,
    sort_order INT,

    CONSTRAINT pk_listing_images PRIMARY KEY (id),
    CONSTRAINT fk_listing_images_listing FOREIGN KEY (listing_id) REFERENCES book_listings (id)
);