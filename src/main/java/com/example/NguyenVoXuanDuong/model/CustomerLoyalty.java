package com.example.NguyenVoXuanDuong.model;

import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customer_loyalty")
public class CustomerLoyalty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerName;

    @Column(unique = true, length = 10)
    private String customerPhone;

    private int points = 0;
}
