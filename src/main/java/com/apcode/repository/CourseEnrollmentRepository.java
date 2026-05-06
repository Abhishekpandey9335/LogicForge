package com.apcode.repository;

import com.apcode.entity.CourseEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {

    Optional<CourseEnrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    Optional<CourseEnrollment> findByUserIdAndCourseSlug(Long userId, String courseSlug);

    List<CourseEnrollment> findByUserId(Long userId);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT COUNT(e) FROM CourseEnrollment e WHERE e.course.id = :courseId")
    long countEnrollmentsByCourseId(Long courseId);

    @Query("SELECT COUNT(DISTINCT e.user.id) FROM CourseEnrollment e")
    long countDistinctStudents();
}
