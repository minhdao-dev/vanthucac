package com.vanthucac.catalog.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "book_catalogs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class BookCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20)
    private String isbn;

    @Column(nullable = false, length = 500)
    private String title;

    private String author;

    private String publisher;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(length = 100)
    private String category;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static BookCatalog create(
            String isbn,
            String title,
            String author,
            String publisher,
            String description,
            String coverUrl,
            String category
    ) {
        var book = new BookCatalog();
        book.isbn = isbn;
        book.title = title;
        book.author = author;
        book.publisher = publisher;
        book.description = description;
        book.coverUrl = coverUrl;
        book.category = category;
        return book;
    }

    public void update(
            String title,
            String author,
            String publisher,
            String description,
            String coverUrl,
            String category
    ) {
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.description = description;
        this.coverUrl = coverUrl;
        this.category = category;
    }
}