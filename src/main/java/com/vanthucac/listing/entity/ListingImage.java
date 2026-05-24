package com.vanthucac.listing.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "listing_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ListingImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private BookListing listing;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "sort_order")
    private Integer sortOrder;

    public static ListingImage create(BookListing listing, String imageUrl, Integer sortOrder) {
        var image = new ListingImage();
        image.listing = listing;
        image.imageUrl = imageUrl;
        image.sortOrder = sortOrder;
        return image;
    }
}