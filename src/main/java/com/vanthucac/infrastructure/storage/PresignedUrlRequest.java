package com.vanthucac.infrastructure.storage;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record PresignedUrlRequest(

        @NotBlank(message = "File name is required")
        String fileName,

        @NotBlank(message = "Content type is required")
        String contentType
) {
    static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    public boolean isContentTypeAllowed() {
        return ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase());
    }
}