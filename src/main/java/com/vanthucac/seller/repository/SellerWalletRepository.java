package com.vanthucac.seller.repository;

import com.vanthucac.seller.entity.SellerWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerWalletRepository extends JpaRepository<SellerWallet, Long> {
    Optional<SellerWallet> findBySellerId(Long sellerId);
}
