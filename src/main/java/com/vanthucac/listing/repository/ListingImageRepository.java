package com.vanthucac.listing.repository;

import com.vanthucac.listing.entity.ListingImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ListingImageRepository extends JpaRepository<ListingImage, Long> {

    List<ListingImage> findByListingIdOrderBySortOrder(Long listingId);

    void deleteByListingId(Long listingId);

    @Query("SELECT i FROM ListingImage i WHERE i.listing.id IN :listingIds ORDER BY i.listing.id, i.sortOrder")
    List<ListingImage> findByListingIdInOrderBySortOrder(@Param("listingIds") Collection<Long> listingIds);
}