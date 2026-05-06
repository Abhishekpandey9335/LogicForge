package com.apcode.repository;

import com.apcode.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Top learners by points for leaderboard
    @Query("SELECT u FROM User u ORDER BY u.totalPoints DESC")
    List<User> findTopLearners(Pageable pageable);

    // All newsletter subscribers
    List<User> findByNewsletterSubscribedTrue();

    Page<User> findByRole(User.Role role, Pageable pageable);
}
