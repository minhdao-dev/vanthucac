package com.vanthucac.listing.repository;

import com.vanthucac.listing.entity.ListingImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListingImageRepository extends JpaRepository<ListingImage, Long> {
    List<ListingImage> findByListingIdOrderBySortOrder(Long listingId);

    void deleteByListingId(Long listingId);
}