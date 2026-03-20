package com.example.NguyenVoXuanDuong.repository;

import com.example.NguyenVoXuanDuong.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
	@Query("select distinct o from Order o left join fetch o.orderDetails od left join fetch od.product where o.id = :id")
	Optional<Order> findByIdWithDetails(@Param("id") Long id);

	List<Order> findByCustomerNameIgnoreCaseOrderByIdDesc(String customerName);
}