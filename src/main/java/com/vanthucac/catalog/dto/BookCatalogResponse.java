package com.vanthucac.catalog.dto;

import com.vanthucac.catalog.entity.BookCatalog;

import java.time.Instant;

public record BookCatalogResponse(
        Long id,
        String isbn,
        String title,
        String author,
        String publisher,
        String description,
        String coverUrl,
        String category,
        Instant createdAt
) {
    public static BookCatalogResponse from(BookCatalog book) {
        return new BookCatalogResponse(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublisher(),
                book.getDescription(),
                book.getCoverUrl(),
                book.getCategory(),
                book.getCreatedAt()
        );
    }
}