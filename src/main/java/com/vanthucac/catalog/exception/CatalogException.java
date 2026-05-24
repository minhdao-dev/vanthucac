package com.vanthucac.catalog.exception;

import com.vanthucac.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class CatalogException extends BusinessException {

    public CatalogException(String message, String errorCode, HttpStatus status) {
        super(message, errorCode, status);
    }

    public static CatalogException bookNotFound() {
        return new CatalogException(
                "Book not found",
                CatalogErrorCode.BOOK_NOT_FOUND,
                HttpStatus.NOT_FOUND
        );
    }

    public static CatalogException isbnAlreadyExists(String isbn) {
        return new CatalogException(
                "ISBN already exists: " + isbn,
                CatalogErrorCode.ISBN_ALREADY_EXISTS,
                HttpStatus.CONFLICT
        );
    }
}