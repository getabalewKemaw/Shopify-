package com.shopapplication.repository;

import com.shopapplication.models.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
// the oprional means may null or not is the means of avoiding the  java null pointere ecexeption 
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);
}
