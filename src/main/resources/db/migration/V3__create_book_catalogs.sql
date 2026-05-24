CREATE TABLE book_catalogs
(
    id          INT          NOT NULL AUTO_INCREMENT,
    isbn        VARCHAR(20),
    title       VARCHAR(500) NOT NULL,
    author      VARCHAR(255),
    publisher   VARCHAR(255),
    description TEXT,
    cover_url   VARCHAR(500),
    category    VARCHAR(100),
    created_at  DATETIME(6)  NOT NULL,

    CONSTRAINT pk_book_catalogs PRIMARY KEY (id),
    CONSTRAINT uq_book_catalogs_isbn UNIQUE (isbn)
);