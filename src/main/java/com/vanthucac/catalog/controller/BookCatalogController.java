package com.vanthucac.catalog.controller;

import com.vanthucac.catalog.dto.BookCatalogResponse;
import com.vanthucac.catalog.dto.CreateBookRequest;
import com.vanthucac.catalog.dto.PageResponse;
import com.vanthucac.catalog.service.BookCatalogService;
import com.vanthucac.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/books")
public class BookCatalogController {

    private final BookCatalogService bookCatalogService;

    public BookCatalogController(BookCatalogService bookCatalogService) {
        this.bookCatalogService = bookCatalogService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BookCatalogResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        var result = bookCatalogService.search(keyword, category, page, size, sort);
        return ResponseEntity.ok(ApiResponse.ok("Books retrieved successfully", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookCatalogResponse>> getById(@PathVariable Long id) {
        var book = bookCatalogService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok("Book retrieved successfully", book));
    }

    @GetMapping("/lookup/{isbn}")
    public ResponseEntity<ApiResponse<BookCatalogResponse>> lookupByIsbn(
            @PathVariable String isbn
    ) {
        var book = bookCatalogService.lookupByIsbn(isbn);
        return ResponseEntity.ok(ApiResponse.ok("Book found", book));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookCatalogResponse>> create(
            @Valid @RequestBody CreateBookRequest request
    ) {
        var book = bookCatalogService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Book added to catalog", book));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BookCatalogResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateBookRequest request
    ) {
        var book = bookCatalogService.update(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Book updated successfully", book));
    }
}