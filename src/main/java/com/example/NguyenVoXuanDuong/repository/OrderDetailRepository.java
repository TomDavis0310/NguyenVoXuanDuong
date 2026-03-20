package com.example.NguyenVoXuanDuong.repository;

import com.example.NguyenVoXuanDuong.model.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
}