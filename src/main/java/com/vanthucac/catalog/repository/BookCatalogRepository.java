package com.vanthucac.catalog.repository;

import com.vanthucac.catalog.entity.BookCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookCatalogRepository extends JpaRepository<BookCatalog, Long>,
        JpaSpecificationExecutor<BookCatalog> {

    Optional<BookCatalog> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);
}