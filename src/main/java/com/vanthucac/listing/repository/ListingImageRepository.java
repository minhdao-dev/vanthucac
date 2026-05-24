package com.vanthucac.listing.repository;

import com.vanthucac.listing.entity.ListingImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListingImageRepository extends JpaRepository<ListingImage, Integer> {
    List<ListingImage> findByListingIdOrderBySortOrder(Integer listingId);

    void deleteByListingId(Integer listingId);
}