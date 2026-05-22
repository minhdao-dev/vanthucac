package com.vanthucac.seller.repository;

import com.vanthucac.auth.entity.User;
import com.vanthucac.seller.entity.SellerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SellerProfileRepository extends JpaRepository<SellerProfile, Integer> {
    boolean existsByUser(User user);
}