package com.example.NguyenVoXuanDuong.repository;

import com.example.NguyenVoXuanDuong.model.CustomerLoyalty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerLoyaltyRepository extends JpaRepository<CustomerLoyalty, Long> {
    Optional<CustomerLoyalty> findByCustomerNameIgnoreCase(String customerName);

    Optional<CustomerLoyalty> findByCustomerPhone(String customerPhone);
}
