package com.vanthucac.catalog.repository;

import com.vanthucac.catalog.entity.BookCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface BookCatalogRepository extends JpaRepository<BookCatalog, Long>,
        JpaSpecificationExecutor<BookCatalog> {

    Optional<BookCatalog> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);
}