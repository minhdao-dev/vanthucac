package com.vanthucac.listing.service;

import com.vanthucac.catalog.entity.BookCatalog;
import com.vanthucac.catalog.exception.CatalogException;
import com.vanthucac.catalog.repository.BookCatalogRepository;
import com.vanthucac.common.dto.PageResponse;
import com.vanthucac.common.util.PageableUtils;
import com.vanthucac.listing.dto.CreateListingRequest;
import com.vanthucac.listing.dto.ListingResponse;
import com.vanthucac.listing.dto.UpdateListingRequest;
import com.vanthucac.listing.entity.BookListing;
import com.vanthucac.listing.entity.ListingImage;
import com.vanthucac.listing.exception.ListingException;
import com.vanthucac.listing.repository.BookListingRepository;
import com.vanthucac.listing.repository.BookListingSpecification;
import com.vanthucac.listing.repository.ListingImageRepository;
import com.vanthucac.seller.entity.SellerProfile;
import com.vanthucac.seller.exception.SellerException;
import com.vanthucac.seller.repository.SellerProfileRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BookListingService {

    private final BookListingRepository bookListingRepository;
    private final ListingImageRepository listingImageRepository;
    private final BookCatalogRepository bookCatalogRepository;
    private final SellerProfileRepository sellerProfileRepository;

    public BookListingService(
            BookListingRepository bookListingRepository,
            ListingImageRepository listingImageRepository,
            BookCatalogRepository bookCatalogRepository,
            SellerProfileRepository sellerProfileRepository
    ) {
        this.bookListingRepository = bookListingRepository;
        this.listingImageRepository = listingImageRepository;
        this.bookCatalogRepository = bookCatalogRepository;
        this.sellerProfileRepository = sellerProfileRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingResponse> search(
            Long bookId,
            Long sellerId,
            String listingType,
            String condition,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sort
    ) {
        var pageable = PageableUtils.build(page, size, sort);

        var spec = Specification.allOf(
                BookListingSpecification.isActive(),
                BookListingSpecification.hasBookId(bookId),
                BookListingSpecification.hasSellerId(sellerId),
                BookListingSpecification.hasListingType(listingType),
                BookListingSpecification.hasCondition(condition),
                BookListingSpecification.hasPriceBetween(minPrice, maxPrice)
        );

        return PageResponse.from(
                bookListingRepository.findAll(spec, pageable)
                        .map(listing -> {
                            var images = getImageUrls(listing.getId());
                            return ListingResponse.from(listing, images);
                        })
        );
    }

    @Transactional(readOnly = true)
    public ListingResponse getById(Long id) {
        var listing = bookListingRepository.findById(id)
                .orElseThrow(ListingException::listingNotFound);
        var images = getImageUrls(id);
        return ListingResponse.from(listing, images);
    }

    @Transactional
    public ListingResponse create(CreateListingRequest request, Jwt jwt) {
        var seller = getSellerFromJwt(jwt);
        var bookCatalog = getBookCatalog(request.bookCatalogId());
        var condition = parseCondition(request.condition());

        var listing = BookListing.createC2C(
                bookCatalog,
                seller,
                request.price(),
                condition,
                request.stock()
        );
        bookListingRepository.save(listing);

        saveImages(listing, request.imageUrls());

        var images = getImageUrls(listing.getId());
        return ListingResponse.from(listing, images);
    }

    @Transactional
    public ListingResponse update(Long id, UpdateListingRequest request, Jwt jwt) {
        var listing = bookListingRepository.findById(id)
                .orElseThrow(ListingException::listingNotFound);

        var sellerId = getSellerIdFromJwt(jwt);
        if (listing.isNotOwnedBy(sellerId)) {
            throw ListingException.accessDenied();
        }

        var condition = request.condition() != null
                ? parseCondition(request.condition())
                : listing.getCondition();

        var status = request.status() != null
                ? parseStatus(request.status())
                : listing.getStatus();

        var price = request.price() != null ? request.price() : listing.getPrice();
        var stock = request.stock() != null ? request.stock() : listing.getStock();

        listing.update(price, condition, stock, status);

        if (request.imageUrls() != null) {
            listingImageRepository.deleteByListingId(id);
            saveImages(listing, request.imageUrls());
        }

        var images = getImageUrls(listing.getId());
        return ListingResponse.from(listing, images);
    }

    @Transactional
    public void deactivate(Long id, Jwt jwt) {
        var listing = bookListingRepository.findById(id)
                .orElseThrow(ListingException::listingNotFound);

        var sellerId = getSellerIdFromJwt(jwt);
        if (listing.isNotOwnedBy(sellerId)) {
            throw ListingException.accessDenied();
        }

        listing.deactivate();
    }

    private SellerProfile getSellerFromJwt(Jwt jwt) {
        var userId = Long.parseLong(jwt.getSubject());
        return sellerProfileRepository.findByUserId(userId)
                .orElseThrow(SellerException::sellerNotFound);
    }

    private Long getSellerIdFromJwt(Jwt jwt) {
        return getSellerFromJwt(jwt).getId();
    }

    private BookCatalog getBookCatalog(Long bookCatalogId) {
        return bookCatalogRepository.findById(bookCatalogId)
                .orElseThrow(CatalogException::bookNotFound);
    }

    private BookListing.BookCondition parseCondition(String condition) {
        try {
            return BookListing.BookCondition.valueOf(condition);
        } catch (IllegalArgumentException e) {
            throw ListingException.invalidCondition(condition);
        }
    }

    private BookListing.ListingStatus parseStatus(String status) {
        try {
            return BookListing.ListingStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw ListingException.invalidStatus(status);
        }
    }

    private void saveImages(BookListing listing, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return;
        for (int i = 0; i < imageUrls.size(); i++) {
            var image = ListingImage.create(listing, imageUrls.get(i), i);
            listingImageRepository.save(image);
        }
    }

    private List<String> getImageUrls(Long listingId) {
        return listingImageRepository
                .findByListingIdOrderBySortOrder(listingId)
                .stream()
                .map(ListingImage::getImageUrl)
                .toList();
    }
}