package com.vanthucac.listing.repository;

import com.vanthucac.listing.entity.BookListing;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookListingRepository extends JpaRepository<BookListing, Long>,
        JpaSpecificationExecutor<BookListing> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM BookListing l WHERE l.id = :id")
    Optional<BookListing> findByIdWithLock(Long id);
}