package com.vanthucac.catalog.service;

import com.vanthucac.catalog.dto.BookCatalogResponse;
import com.vanthucac.catalog.dto.CreateBookRequest;
import com.vanthucac.catalog.dto.PageResponse;
import com.vanthucac.catalog.entity.BookCatalog;
import com.vanthucac.catalog.exception.CatalogException;
import com.vanthucac.catalog.repository.BookCatalogRepository;
import com.vanthucac.catalog.repository.BookCatalogSpecification;
import com.vanthucac.common.util.PageableUtils;
import com.vanthucac.infrastructure.external.GoogleBooksApiClient;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookCatalogService {

    private final BookCatalogRepository bookCatalogRepository;
    private final GoogleBooksApiClient googleBooksApiClient;

    public BookCatalogService(
            BookCatalogRepository bookCatalogRepository,
            GoogleBooksApiClient googleBooksApiClient
    ) {
        this.bookCatalogRepository = bookCatalogRepository;
        this.googleBooksApiClient = googleBooksApiClient;
    }

    public PageResponse<BookCatalogResponse> search(
            String keyword,
            String category,
            int page,
            int size,
            String sort
    ) {
        var pageable = PageableUtils.build(page, size, sort);

        var spec = Specification.allOf(
                BookCatalogSpecification.hasKeyword(keyword),
                BookCatalogSpecification.hasCategory(category)
        );

        return PageResponse.from(
                bookCatalogRepository.findAll(spec, pageable)
                        .map(BookCatalogResponse::from)
        );
    }

    public BookCatalogResponse getById(Long id) {
        return bookCatalogRepository.findById(id)
                .map(BookCatalogResponse::from)
                .orElseThrow(CatalogException::bookNotFound);
    }

    public BookCatalogResponse lookupByIsbn(String isbn) {
        var existing = bookCatalogRepository.findByIsbn(isbn);
        if (existing.isPresent()) {
            return BookCatalogResponse.from(existing.get());
        }

        var googleResult = googleBooksApiClient.findByIsbn(isbn);
        if (googleResult.isEmpty()) {
            throw CatalogException.bookNotFound();
        }

        var info = googleResult.get();
        var book = BookCatalog.create(
                info.isbn(),
                info.title(),
                info.author(),
                info.publisher(),
                info.description(),
                info.coverUrl(),
                info.category()
        );
        bookCatalogRepository.save(book);

        return BookCatalogResponse.from(book);
    }

    @Transactional
    public BookCatalogResponse create(CreateBookRequest request) {
        if (request.isbn() != null && bookCatalogRepository.existsByIsbn(request.isbn())) {
            throw CatalogException.isbnAlreadyExists(request.isbn());
        }

        var book = BookCatalog.create(
                request.isbn(),
                request.title(),
                request.author(),
                request.publisher(),
                request.description(),
                request.coverUrl(),
                request.category()
        );

        bookCatalogRepository.save(book);
        return BookCatalogResponse.from(book);
    }

    @Transactional
    public BookCatalogResponse update(Long id, CreateBookRequest request) {
        var book = bookCatalogRepository.findById(id)
                .orElseThrow(CatalogException::bookNotFound);

        book.update(
                request.title(),
                request.author(),
                request.publisher(),
                request.description(),
                request.coverUrl(),
                request.category()
        );

        return BookCatalogResponse.from(book);
    }
}