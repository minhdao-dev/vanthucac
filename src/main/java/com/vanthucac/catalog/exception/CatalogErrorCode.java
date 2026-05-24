package com.vanthucac.catalog.exception;

public final class CatalogErrorCode {
    private CatalogErrorCode() {
    }

    public static final String BOOK_NOT_FOUND = "CATALOG_BOOK_NOT_FOUND";
    public static final String ISBN_ALREADY_EXISTS = "CATALOG_ISBN_ALREADY_EXISTS";
}