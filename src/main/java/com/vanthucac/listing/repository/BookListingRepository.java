package com.vanthucac.listing.repository;

import com.vanthucac.listing.entity.BookListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BookListingRepository extends JpaRepository<BookListing, Long>,
        JpaSpecificationExecutor<BookListing> {
}