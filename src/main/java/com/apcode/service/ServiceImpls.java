package com.apcode.service;

import com.apcode.dto.*;
import com.apcode.entity.*;
import com.apcode.exception.*;
import com.apcode.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;

// ─────────────────────────────────────────────────────────────
//  Video Service
// ─────────────────────────────────────────────────────────────
@Service
@RequiredArgsConstructor
class VideoServiceImpl implements VideoService {

    private final VideoRepository videoRepository;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public List<VideoResponse> getVideosByCourse(String courseSlug) {
        return videoRepository.findByCourseSlugOrderByOrderIndexAsc(courseSlug)
                .stream().map(this::map).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VideoResponse> getFreeVideos() {
        return videoRepository.findFreeVideos()
                .stream().map(this::map).collect(Collectors.toList());
    }

    @Transactional
    public VideoResponse createVideo(CreateVideoRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        Video video = Video.builder()
                .youtubeId(request.getYoutubeId())
                .title(request.getTitle())
                .description(request.getDescription())
                .thumbnailUrl(request.getThumbnailUrl() != null
                        ? request.getThumbnailUrl()
                        : "https://img.youtube.com/vi/" + request.getYoutubeId() + "/mqdefault.jpg")
                .orderIndex(request.getOrderIndex())
                .isFree(request.getIsFree())
                .course(course)
                .build();

        // Update total lecture count
        course.setTotalLectures(course.getTotalLectures() + 1);
        courseRepository.save(course);

        return map(videoRepository.save(video));
    }

    private VideoResponse map(Video v) {
        return VideoResponse.builder()
                .id(v.getId())
                .youtubeId(v.getYoutubeId())
                .title(v.getTitle())
                .description(v.getDescription())
                .thumbnailUrl(v.getThumbnailUrl())
                .orderIndex(v.getOrderIndex())
                .isFree(v.getIsFree())
                .courseSlug(v.getCourse().getSlug())
                .isCompleted(false) // populated in CourseService if needed
                .build();
    }
}

// ─────────────────────────────────────────────────────────────
//  Newsletter Service
// ─────────────────────────────────────────────────────────────
@Service
@RequiredArgsConstructor
class NewsletterServiceImpl implements NewsletterService {

    private final NewsletterSubscriberRepository repo;

    @Transactional
    public String subscribe(String email) {
        if (repo.existsByEmail(email)) {
            // Re-activate if previously unsubscribed
            repo.findByEmail(email).ifPresent(s -> {
                if (!s.getActive()) { s.setActive(true); repo.save(s); }
            });
            return "Already subscribed — welcome back!";
        }
        NewsletterSubscriber subscriber = NewsletterSubscriber.builder()
                .email(email)
                .active(true)
                .build();
        repo.save(subscriber);
        return "Subscribed successfully! Welcome to AP_Code community.";
    }

    @Transactional
    public void unsubscribe(String email) {
        repo.findByEmail(email).ifPresent(s -> {
            s.setActive(false);
            repo.save(s);
        });
    }

    @Transactional(readOnly = true)
    public long countSubscribers() {
        return repo.countByActiveTrue();
    }
}

// ─────────────────────────────────────────────────────────────
//  Review Service
// ─────────────────────────────────────────────────────────────
@Service
@RequiredArgsConstructor
class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public List<ReviewResponse> getPublicReviews() {
        return reviewRepository.findByApprovedTrueOrderByCreatedAtDesc()
                .stream().map(this::map).collect(Collectors.toList());
    }

    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Review review = Review.builder()
                .user(user)
                .reviewText(request.getReviewText())
                .rating(request.getRating())
                .approved(false) // pending admin approval
                .build();

        if (request.getCourseId() != null) {
            Course course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
            review.setCourse(course);
        }

        return map(reviewRepository.save(review));
    }

    // Admin: approve a review
    @Transactional
    public ReviewResponse approveReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        review.setApproved(true);
        return map(reviewRepository.save(review));
    }

    private ReviewResponse map(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .reviewerName(r.getUser().getFullName())
                .city(r.getUser().getCity())
                .rating(r.getRating())
                .reviewText(r.getReviewText())
                .courseTitle(r.getCourse() != null ? r.getCourse().getTitle() : null)
                .createdAt(r.getCreatedAt())
                .build();
    }
}

// ─────────────────────────────────────────────────────────────
//  Leaderboard Service
// ─────────────────────────────────────────────────────────────
@Service
@RequiredArgsConstructor
class LeaderboardServiceImpl implements LeaderboardService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getTopLearners(int limit) {
        AtomicInteger rank = new AtomicInteger(1);
        return userRepository.findTopLearners(PageRequest.of(0, limit))
                .stream()
                .map(u -> LeaderboardEntry.builder()
                        .rank(rank.getAndIncrement())
                        .userId(u.getId())
                        .name(u.getFullName())
                        .city(u.getCity())
                        .totalPoints(u.getTotalPoints())
                        .streakDays(u.getCurrentStreakDays())
                        .build())
                .collect(Collectors.toList());
    }
}

// ─────────────────────────────────────────────────────────────
//  Stats Service
// ─────────────────────────────────────────────────────────────
@Service
@RequiredArgsConstructor
class StatsServiceImpl implements StatsService {

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final CourseRepository courseRepository;
    private final NewsletterSubscriberRepository newsletterRepo;
    private final ReviewRepository reviewRepository;
    private final CourseEnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public PlatformStats getStats() {
        return PlatformStats.builder()
                .totalStudents(enrollmentRepository.countDistinctStudents())
                .totalVideos(videoRepository.count())
                .totalCourses(courseRepository.count())
                .totalSubscribers(newsletterRepo.countByActiveTrue())
                .averageRating(reviewRepository.findAverageRating())
                .build();
    }
}

// ─────────────────────────────────────────────────────────────
//  User Service (profile management)
// ─────────────────────────────────────────────────────────────
@Service
@RequiredArgsConstructor
class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CourseEnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public UserProfile getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<EnrollmentSummary> enrollments = enrollmentRepository
                .findByUserId(user.getId()).stream()
                .map(e -> EnrollmentSummary.builder()
                        .enrollmentId(e.getId())
                        .courseSlug(e.getCourse().getSlug())
                        .courseTitle(e.getCourse().getTitle())
                        .progressPercent(e.getProgressPercent())
                        .enrolledAt(e.getEnrolledAt())
                        .lastAccessedAt(e.getLastAccessedAt())
                        .build())
                .collect(Collectors.toList());

        return UserProfile.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .city(user.getCity())
                .totalPoints(user.getTotalPoints())
                .currentStreakDays(user.getCurrentStreakDays())
                .newsletterSubscribed(user.getNewsletterSubscribed())
                .createdAt(user.getCreatedAt())
                .enrollments(enrollments)
                .build();
    }
}
