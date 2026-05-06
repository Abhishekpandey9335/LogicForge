package com.apcode.repository;

import com.apcode.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findBySlug(String slug);

    List<Course> findByIsPublishedTrueOrderByIdAsc();

    boolean existsBySlug(String slug);
}
