package com.apcode.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course; // null = platform-level review

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reviewText;

    @Column(nullable = false)
    private Integer rating; // 1–5

    @Builder.Default
    private Boolean approved = false; // Admin approves before public display

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
