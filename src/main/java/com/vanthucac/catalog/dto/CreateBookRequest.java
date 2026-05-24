package com.vanthucac.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateBookRequest(
        String isbn,

        @NotBlank(message = "Title is required")
        String title,

        String author,
        String publisher,
        String description,
        String coverUrl,
        String category
) {
}