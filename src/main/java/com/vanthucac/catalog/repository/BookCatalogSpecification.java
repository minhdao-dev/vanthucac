package com.vanthucac.catalog.repository;

import com.vanthucac.catalog.entity.BookCatalog;
import org.springframework.data.jpa.domain.Specification;

public final class BookCatalogSpecification {

    private BookCatalogSpecification() {
    }

    public static Specification<BookCatalog> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            var pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("author")), pattern),
                    cb.like(cb.lower(root.get("isbn")), pattern)
            );
        };
    }

    public static Specification<BookCatalog> hasCategory(String category) {
        return (root, query, cb) -> {
            if (category == null || category.isBlank()) return null;
            return cb.equal(root.get("category"), category);
        };
    }
}