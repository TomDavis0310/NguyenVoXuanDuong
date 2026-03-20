package com.example.NguyenVoXuanDuong.model;

import jakarta.persistence.*;
import lombok.*;
@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private double price;
    private String description;
    private String image;
    private boolean isPromotional = false;
    private Double promotionalPrice;
    private Integer promotionalStock = 0;
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
}
