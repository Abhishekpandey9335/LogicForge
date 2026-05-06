package com.apcode.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

// ─────────────────────────────────────────────────────────────
//  AUTH DTOs
// ─────────────────────────────────────────────────────────────

class AuthDTOs {

    @Getter @Setter
    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;
    }

    @Getter @Setter
    public static class RegisterRequest {
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 100)
        private String fullName;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        private String city;
    }

    @Getter @Setter @Builder
    public static class AuthResponse {
        private String token;
        private String tokenType;
        private Long userId;
        private String fullName;
        private String email;
        private String role;
        private Integer totalPoints;
    }
}

// ─────────────────────────────────────────────────────────────
//  USER DTOs
// ─────────────────────────────────────────────────────────────

class UserDTOs {

    @Getter @Setter @Builder
    public static class UserProfile {
        private Long id;
        private String fullName;
        private String email;
        private String city;
        private Integer totalPoints;
        private Integer currentStreakDays;
        private Boolean newsletterSubscribed;
        private LocalDateTime createdAt;
        private List<EnrollmentDTOs.EnrollmentSummary> enrollments;
    }

    @Getter @Setter
    public static class UpdateProfileRequest {
        @Size(min = 2, max = 100)
        private String fullName;
        private String city;
    }
}

// ─────────────────────────────────────────────────────────────
//  COURSE DTOs
// ─────────────────────────────────────────────────────────────

class CourseDTOs {

    @Getter @Setter @Builder
    public static class CourseResponse {
        private Long id;
        private String slug;
        private String title;
        private String description;
        private String icon;
        private String badge;
        private Integer totalLectures;
        private Boolean isFree;
        private Long enrollmentCount;
        // User-specific (null if not authenticated)
        private Integer userProgressPercent;
        private Boolean isEnrolled;
    }

    @Getter @Setter
    public static class CreateCourseRequest {
        @NotBlank private String slug;
        @NotBlank private String title;
        private String description;
        private String icon;
        private String badge;
        private Boolean isFree = true;
    }
}

// ─────────────────────────────────────────────────────────────
//  VIDEO DTOs
// ─────────────────────────────────────────────────────────────

class VideoDTOs {

    @Getter @Setter @Builder
    public static class VideoResponse {
        private Long id;
        private String youtubeId;
        private String title;
        private String description;
        private String thumbnailUrl;
        private Integer orderIndex;
        private Boolean isFree;
        private String courseSlug;
        private Boolean isCompleted; // user-specific
    }

    @Getter @Setter
    public static class CreateVideoRequest {
        @NotBlank private String youtubeId;
        @NotBlank private String title;
        private String description;
        private String thumbnailUrl;
        private Integer orderIndex = 0;
        private Boolean isFree = true;
        @NotNull private Long courseId;
    }
}

// ─────────────────────────────────────────────────────────────
//  ENROLLMENT DTOs
// ─────────────────────────────────────────────────────────────

class EnrollmentDTOs {

    @Getter @Setter @Builder
    public static class EnrollmentSummary {
        private Long enrollmentId;
        private String courseSlug;
        private String courseTitle;
        private Integer progressPercent;
        private LocalDateTime enrolledAt;
        private LocalDateTime lastAccessedAt;
    }

    @Getter @Setter
    public static class MarkCompleteRequest {
        @NotNull private Long videoId;
    }
}

// ─────────────────────────────────────────────────────────────
//  REVIEW DTOs
// ─────────────────────────────────────────────────────────────

class ReviewDTOs {

    @Getter @Setter @Builder
    public static class ReviewResponse {
        private Long id;
        private String reviewerName;
        private String city;
        private Integer rating;
        private String reviewText;
        private String courseTitle;
        private LocalDateTime createdAt;
    }

    @Getter @Setter
    public static class CreateReviewRequest {
        @NotBlank private String reviewText;
        @NotNull @Min(1) @Max(5) private Integer rating;
        private Long courseId; // optional — null = platform review
    }
}

// ─────────────────────────────────────────────────────────────
//  LEADERBOARD DTOs
// ─────────────────────────────────────────────────────────────

class LeaderboardDTOs {

    @Getter @Setter @Builder
    public static class LeaderboardEntry {
        private Integer rank;
        private Long userId;
        private String name;
        private String city;
        private Integer totalPoints;
        private Integer streakDays;
    }
}

// ─────────────────────────────────────────────────────────────
//  NEWSLETTER DTOs
// ─────────────────────────────────────────────────────────────

class NewsletterDTOs {

    @Getter @Setter
    public static class SubscribeRequest {
        @NotBlank @Email(message = "Invalid email")
        private String email;
    }
}

// ─────────────────────────────────────────────────────────────
//  STATS DTO (Homepage counters)
// ─────────────────────────────────────────────────────────────

class StatsDTOs {

    @Getter @Setter @Builder
    public static class PlatformStats {
        private Long totalStudents;
        private Long totalVideos;
        private Long totalCourses;
        private Long totalSubscribers;
        private Double averageRating;
    }
}

// ─────────────────────────────────────────────────────────────
//  GENERIC RESPONSE WRAPPER
// ─────────────────────────────────────────────────────────────

class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
}
