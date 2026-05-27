package com.vanthucac.listing.service;

import com.vanthucac.common.config.CacheConfig;
import com.vanthucac.common.dto.PageResponse;
import com.vanthucac.common.util.PageableUtils;
import com.vanthucac.listing.dto.ListingResponse;
import com.vanthucac.listing.entity.BookListing;
import com.vanthucac.listing.entity.ListingImage;
import com.vanthucac.listing.exception.ListingErrorCode;
import com.vanthucac.listing.exception.ListingException;
import com.vanthucac.listing.repository.BookListingRepository;
import com.vanthucac.listing.repository.ListingImageRepository;
import com.vanthucac.notification.outbox.NotificationOutboxEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminListingService {

    private static final Logger log = LoggerFactory.getLogger(AdminListingService.class);

    private final BookListingRepository bookListingRepository;
    private final ListingImageRepository listingImageRepository;
    private final NotificationOutboxEventPublisher notificationOutboxEventPublisher;

    public AdminListingService(
            BookListingRepository bookListingRepository,
            ListingImageRepository listingImageRepository,
            NotificationOutboxEventPublisher notificationOutboxEventPublisher
    ) {
        this.bookListingRepository = bookListingRepository;
        this.listingImageRepository = listingImageRepository;
        this.notificationOutboxEventPublisher = notificationOutboxEventPublisher;
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingResponse> getPendingListings(int page, int size) {
        var pageable = PageableUtils.build(page, size, "createdAt,asc");

        Specification<BookListing> pendingSpec = (root, query, cb) ->
                cb.equal(root.get("status"), BookListing.ListingStatus.PENDING_REVIEW);

        var listingsPage = bookListingRepository.findAll(pendingSpec, pageable);

        var imagesByListingId = batchLoadImages(
                listingsPage.getContent().stream().map(BookListing::getId).toList()
        );

        return PageResponse.from(
                listingsPage.map(listing -> ListingResponse.from(
                        listing,
                        imagesByListingId.getOrDefault(listing.getId(), List.of())
                ))
        );
    }

    @Caching(evict = {
            @CacheEvict(value = CacheConfig.LISTINGS_CACHE, allEntries = true),
            @CacheEvict(value = CacheConfig.LISTING_DETAIL_CACHE, key = "#listingId")
    })
    @Transactional
    public ListingResponse approveListing(Long listingId) {
        var listing = bookListingRepository.findById(listingId)
                .orElseThrow(ListingException::listingNotFound);

        if (listing.getStatus() != BookListing.ListingStatus.PENDING_REVIEW) {
            throw new ListingException(
                    "Only PENDING_REVIEW listings can be approved",
                    ListingErrorCode.INVALID_STATUS,
                    HttpStatus.BAD_REQUEST
            );
        }

        listing.approve();
        log.info("Listing {} approved by admin", listingId);

        if (listing.getSeller() != null) {
            notificationOutboxEventPublisher.publishListingApprovedNotification(
                    listing.getSeller().getUser().getId(),
                    listingId
            );
        }

        return ListingResponse.from(listing, getImageUrls(listingId));
    }

    @Caching(evict = {
            @CacheEvict(value = CacheConfig.LISTINGS_CACHE, allEntries = true),
            @CacheEvict(value = CacheConfig.LISTING_DETAIL_CACHE, key = "#listingId")
    })
    @Transactional
    public ListingResponse rejectListing(Long listingId, String reason) {
        var listing = bookListingRepository.findById(listingId)
                .orElseThrow(ListingException::listingNotFound);

        if (listing.getStatus() != BookListing.ListingStatus.PENDING_REVIEW) {
            throw new ListingException(
                    "Only PENDING_REVIEW listings can be rejected",
                    ListingErrorCode.INVALID_STATUS,
                    HttpStatus.BAD_REQUEST
            );
        }

        listing.reject();
        log.info("Listing {} rejected by admin — reason: {}", listingId, reason);

        if (listing.getSeller() != null) {
            notificationOutboxEventPublisher.publishListingRejectedNotification(
                    listing.getSeller().getUser().getId(),
                    listingId,
                    reason
            );
        }

        return ListingResponse.from(listing, getImageUrls(listingId));
    }

    private Map<Long, List<String>> batchLoadImages(List<Long> listingIds) {
        if (listingIds.isEmpty()) return Map.of();

        return listingImageRepository
                .findByListingIdInOrderBySortOrder(listingIds)
                .stream()
                .collect(Collectors.groupingBy(
                        img -> img.getListing().getId(),
                        Collectors.mapping(ListingImage::getImageUrl, Collectors.toList())
                ));
    }

    private List<String> getImageUrls(Long listingId) {
        return listingImageRepository
                .findByListingIdOrderBySortOrder(listingId)
                .stream()
                .map(ListingImage::getImageUrl)
                .toList();
    }
}