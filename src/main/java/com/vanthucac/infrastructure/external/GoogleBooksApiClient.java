package com.vanthucac.infrastructure.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Component
public class GoogleBooksApiClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleBooksApiClient.class);
    private static final String BASE_URL = "https://www.googleapis.com/books/v1";

    private final RestClient restClient;
    private final String apiKey;

    public GoogleBooksApiClient(@Value("${app.google-books.api-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .build();
        this.apiKey = apiKey;
    }

    public Optional<GoogleBookInfo> findByIsbn(String isbn) {
        try {
            var response = restClient.get()
                    .uri("/volumes?q=isbn:{isbn}&key={key}", isbn, apiKey)
                    .retrieve()
                    .body(GoogleBooksResponse.class);

            if (response == null
                    || response.totalItems() == 0
                    || response.items() == null
                    || response.items().isEmpty()) {
                return Optional.empty();
            }

            var volumeInfo = response.items().getFirst().volumeInfo();
            if (volumeInfo == null) return Optional.empty();

            return Optional.of(new GoogleBookInfo(
                    isbn,
                    volumeInfo.title(),
                    volumeInfo.authors() != null
                            ? String.join(", ", volumeInfo.authors())
                            : null,
                    volumeInfo.publisher(),
                    volumeInfo.description(),
                    volumeInfo.imageLinks() != null
                            ? volumeInfo.imageLinks().thumbnail()
                            : null,
                    volumeInfo.categories() != null && !volumeInfo.categories().isEmpty()
                            ? volumeInfo.categories().getFirst()
                            : null
            ));
        } catch (Exception e) {
            log.warn("Google Books API failed for ISBN {}: {}", isbn, e.getMessage());
            return Optional.empty();
        }
    }

    public record GoogleBookInfo(
            String isbn,
            String title,
            String author,
            String publisher,
            String description,
            String coverUrl,
            String category
    ) {
    }

    record GoogleBooksResponse(int totalItems, List<Item> items) {
    }

    record Item(VolumeInfo volumeInfo) {
    }

    record VolumeInfo(
            String title,
            List<String> authors,
            String publisher,
            String description,
            ImageLinks imageLinks,
            List<String> categories
    ) {
    }

    record ImageLinks(String thumbnail) {
    }
}