package com.vanthucac.seller.service;

import com.vanthucac.auth.entity.Role;
import com.vanthucac.auth.repository.RoleRepository;
import com.vanthucac.auth.repository.UserRepository;
import com.vanthucac.auth.service.TokenService;
import com.vanthucac.seller.dto.SellerProfileResponse;
import com.vanthucac.seller.dto.UpgradeSellerResponse;
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
    private final RoleRepository roleRepository;
    private final TokenService tokenService;

    public SellerService(
            SellerProfileRepository sellerProfileRepository,
            SellerWalletRepository sellerWalletRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            TokenService tokenService
    ) {
        this.sellerProfileRepository = sellerProfileRepository;
        this.sellerWalletRepository = sellerWalletRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tokenService = tokenService;
    }

    @Transactional
    public UpgradeSellerResponse upgradeSeller(UpgradeSellerRequest request, Jwt jwt) {
        var userId = extractUserId(jwt);
        var user = userRepository.findById(userId)
                .orElseThrow(UserException::userNotFound);

        if (sellerProfileRepository.existsByUser(user)) {
            throw SellerException.alreadySeller();
        }

        var sellerRole = roleRepository.findByName(Role.RoleName.SELLER)
                .orElseThrow(() -> new IllegalStateException("Role SELLER not found — check V1 migration"));

        user.addRole(sellerRole);

        var sellerProfile = SellerProfile.create(user, request.shopName(), request.description());
        sellerProfileRepository.save(sellerProfile);

        var wallet = SellerWallet.create(sellerProfile);
        sellerWalletRepository.save(wallet);

        var sessionId = jwt.getClaim("sessionId").toString();
        var newAccessToken = tokenService.generateAccessToken(user, sessionId);

        return UpgradeSellerResponse.of(SellerProfileResponse.from(sellerProfile), newAccessToken);
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