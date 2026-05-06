package com.apcode.repository;

import com.apcode.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    List<Video> findByCourseIdOrderByOrderIndexAsc(Long courseId);

    List<Video> findByCourseSlugOrderByOrderIndexAsc(String courseSlug);

    // Free videos for gallery (first N free videos)
    @Query("SELECT v FROM Video v WHERE v.isFree = true ORDER BY v.createdAt DESC")
    List<Video> findFreeVideos();

    long countByCourseId(Long courseId);
}
