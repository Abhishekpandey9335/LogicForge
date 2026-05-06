package com.apcode.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "course_enrollments",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id"})
)
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // Completed video IDs stored as comma-separated or use a separate table
    @ElementCollection
    @CollectionTable(name = "completed_videos",
        joinColumns = @JoinColumn(name = "enrollment_id"))
    @Column(name = "video_id")
    @Builder.Default
    private List<Long> completedVideoIds = new ArrayList<>();

    @Builder.Default
    private Integer progressPercent = 0; // 0–100

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime enrolledAt;

    @LastModifiedDate
    private LocalDateTime lastAccessedAt;
}
