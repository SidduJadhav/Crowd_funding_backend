//package com.instagram.backend.repository;
//
//import com.example.securitydemo.model.User;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Modifying;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.Optional;
//
//@Repository
//public interface UserRepository extends JpaRepository<User, Long> {
//    Optional<User> findByUsername(String username);
//    Optional<User> findByEmail(String email);
//    Boolean existsByUsername(String username);
//    Boolean existsByEmail(String email);
//    // Add this to your repository interface
//    @Modifying
//    @Query("UPDATE User u SET u.role = 'ROLE_ADMIN' WHERE u.username = :username")
//    void promoteToAdmin(@Param("username") String username);
//}