package com.shopapplication.repository;

import com.shopapplication.models.Favorite;
import com.shopapplication.models.User;
import com.shopapplication.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUser(User user);
    List<Favorite> findByUserId(Long userId);
    Optional<Favorite> findByUserAndProduct(User user, Product product);
    Optional<Favorite> findByUserIdAndProductId(Long userId, Long productId);
    boolean existsByUserAndProduct(User user, Product product);
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    void deleteByUserAndProduct(User user, Product product);
    void deleteByUserIdAndProductId(Long userId, Long productId);
    long countByUserId(Long userId);
}
