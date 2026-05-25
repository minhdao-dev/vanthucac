package com.vanthucac.seller.service;

import com.vanthucac.auth.repository.UserRepository;
import com.vanthucac.seller.dto.SellerProfileResponse;
import com.vanthucac.seller.dto.WalletResponse;
import com.vanthucac.seller.entity.SellerProfile;
import com.vanthucac.seller.entity.SellerWallet;
import com.vanthucac.seller.exception.SellerException;
import com.vanthucac.seller.repository.SellerProfileRepository;
import com.vanthucac.seller.repository.SellerWalletRepository;
import com.vanthucac.user.dto.UpgradeSellerRequest;
import com.vanthucac.user.exception.UserException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SellerService {

    private final SellerProfileRepository sellerProfileRepository;
    private final SellerWalletRepository sellerWalletRepository;
    private final UserRepository userRepository;

    public SellerService(
            SellerProfileRepository sellerProfileRepository,
            SellerWalletRepository sellerWalletRepository,
            UserRepository userRepository
    ) {
        this.sellerProfileRepository = sellerProfileRepository;
        this.sellerWalletRepository = sellerWalletRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SellerProfileResponse upgradeSeller(UpgradeSellerRequest request, Jwt jwt) {
        var userId = extractUserId(jwt);
        var user = userRepository.findById(userId)
                .orElseThrow(UserException::userNotFound);

        if (sellerProfileRepository.existsByUser(user)) {
            throw SellerException.alreadySeller();
        }

        var sellerProfile = SellerProfile.create(user, request.shopName(), request.description());
        sellerProfileRepository.save(sellerProfile);

        var wallet = SellerWallet.create(sellerProfile);
        sellerWalletRepository.save(wallet);

        return SellerProfileResponse.from(sellerProfile);
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(Jwt jwt) {
        var userId = extractUserId(jwt);
        var sellerProfile = sellerProfileRepository.findByUserId(userId)
                .orElseThrow(SellerException::sellerNotFound);

        var wallet = sellerWalletRepository.findBySellerId(sellerProfile.getId())
                .orElseThrow(SellerException::sellerNotFound);

        return WalletResponse.from(wallet);
    }

    private Long extractUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}