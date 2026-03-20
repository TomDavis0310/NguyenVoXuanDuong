package com.example.NguyenVoXuanDuong.repository;

import com.example.NguyenVoXuanDuong.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
	List<Category> findByParentCategoryIsNullOrderByNameAsc();

	List<Category> findByParentCategoryIsNotNullOrderByNameAsc();

	boolean existsByParentCategoryId(Long parentId);

	@Query("select distinct c from Category c left join fetch c.children where c.parentCategory is null order by c.name asc")
	List<Category> findRootCategoriesWithChildren();
}