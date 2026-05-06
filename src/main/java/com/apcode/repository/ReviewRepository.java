package com.apcode.repository;

import com.apcode.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Public approved reviews (for homepage)
    List<Review> findByApprovedTrueOrderByCreatedAtDesc();

    // Reviews for a specific course
    List<Review> findByCourseIdAndApprovedTrueOrderByCreatedAtDesc(Long courseId);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.approved = true")
    Double findAverageRating();

    @Query("SELECT COUNT(r) FROM Review r WHERE r.approved = true")
    long countApprovedReviews();
}
