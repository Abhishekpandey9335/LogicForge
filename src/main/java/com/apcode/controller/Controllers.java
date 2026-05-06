package com.apcode.controller;

import com.apcode.dto.*;
import com.apcode.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ─────────────────────────────────────────────────────────────
//  Course Controller
// ─────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
class CourseController {
    // CORRECT — Spring resolves the impl automatically
    @Autowired
    private VideoService videoService;

    @Autowired
    private UserService userService;

    @Autowired
    private NewsletterService newsletterService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private LeaderboardService leaderboardService;

    @Autowired
    private StatsService statsService;

    private final CourseService courseService;

    /** GET /api/courses — all published courses */
    @GetMapping
    public ApiResponse<List<CourseResponse>> listCourses(
            @AuthenticationPrincipal UserDetails user) {
        String email = user != null ? user.getUsername() : null;
        return ApiResponse.ok(courseService.getAllCourses(email));
    }

    /** GET /api/courses/{slug} */
    @GetMapping("/{slug}")
    public ApiResponse<CourseResponse> getCourse(
            @PathVariable String slug,
            @AuthenticationPrincipal UserDetails user) {
        String email = user != null ? user.getUsername() : null;
        return ApiResponse.ok(courseService.getCourseBySlug(slug, email));
    }

    /** POST /api/courses — Admin only */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CourseResponse> createCourse(
            @Valid @RequestBody CreateCourseRequest request) {
        return ApiResponse.ok("Course created", courseService.createCourse(request));
    }

    /** POST /api/courses/{slug}/enroll — Authenticated */
    @PostMapping("/{slug}/enroll")
    public ApiResponse<EnrollmentSummary> enroll(
            @PathVariable String slug,
            @AuthenticationPrincipal UserDetails user) {
        return ApiResponse.ok("Enrolled successfully!",
                courseService.enrollUser(slug, user.getUsername()));
    }

    /** POST /api/courses/{slug}/complete — mark video as watched */
    @PostMapping("/{slug}/complete")
    public ApiResponse<EnrollmentSummary> markComplete(
            @PathVariable String slug,
            @Valid @RequestBody MarkCompleteRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ApiResponse.ok("Progress updated!",
                courseService.markVideoComplete(slug, request.getVideoId(), user.getUsername()));
    }

    /** GET /api/courses/my/enrollments */
    @GetMapping("/my/enrollments")
    public ApiResponse<List<EnrollmentSummary>> myEnrollments(
            @AuthenticationPrincipal UserDetails user) {
        return ApiResponse.ok(courseService.getUserEnrollments(user.getUsername()));
    }
}

// ─────────────────────────────────────────────────────────────
//  Video Controller
// ─────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
class VideoController {
    @Autowired

    private VideoService videoService;

    /** GET /api/videos/free — public free videos (gallery) */
    @GetMapping("/free")
    public ApiResponse<List<VideoResponse>> freeVideos() {
        return ApiResponse.ok(videoService.getFreeVideos());
    }

    /** GET /api/videos/course/{slug} */
    @GetMapping("/course/{slug}")
    public ApiResponse<List<VideoResponse>> byCourse(@PathVariable String slug) {
        return ApiResponse.ok(videoService.getVideosByCourse(slug));
    }

    /** POST /api/videos — Admin only */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<VideoResponse> createVideo(
            @Valid @RequestBody CreateVideoRequest request) {
        return ApiResponse.ok("Video added", videoService.createVideo(request));
    }
}

// ─────────────────────────────────────────────────────────────
//  User/Profile Controller
// ─────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
class UserController {
    @Autowired
    private UserService userService;

    /** GET /api/users/me */
    @GetMapping("/me")
    public ApiResponse<UserProfile> myProfile(@AuthenticationPrincipal UserDetails user) {
        return ApiResponse.ok(userService.getProfile(user.getUsername()));
    }
}

// ─────────────────────────────────────────────────────────────
//  Newsletter Controller
// ─────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/newsletter")
@RequiredArgsConstructor
class NewsletterController {

    @Autowired
    private NewsletterService newsletterService;

    /** POST /api/newsletter/subscribe */
    @PostMapping("/subscribe")
    public ApiResponse<Void> subscribe(@Valid @RequestBody NewsletterRequest request) {
        String msg = newsletterService.subscribe(request.getEmail());
        return ApiResponse.ok(msg, null);
    }

    /** POST /api/newsletter/unsubscribe */
    @PostMapping("/unsubscribe")
    public ApiResponse<Void> unsubscribe(@RequestBody NewsletterRequest request) {
        newsletterService.unsubscribe(request.getEmail());
        return ApiResponse.ok("Unsubscribed successfully", null);
    }
}

// ─────────────────────────────────────────────────────────────
//  Review Controller
// ─────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
class ReviewController {
    @Autowired
    private ReviewService reviewService;

    /** GET /api/reviews/public — approved reviews for homepage */
    @GetMapping("/public")
    public ApiResponse<List<ReviewResponse>> publicReviews() {
        return ApiResponse.ok(reviewService.getPublicReviews());
    }

    /** POST /api/reviews */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewResponse> submitReview(
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ApiResponse.ok("Review submitted — pending approval",
                reviewService.createReview(request, user.getUsername()));
    }

    /** PATCH /api/reviews/{id}/approve — Admin only */
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReviewResponse> approveReview(@PathVariable Long id) {
        return ApiResponse.ok("Review approved", reviewService.approveReview(id));
    }
}

// ─────────────────────────────────────────────────────────────
//  Leaderboard Controller
// ─────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/leaderboard")
@RequiredArgsConstructor
class LeaderboardController {

    @Autowired
    private LeaderboardService leaderboardService;

    /** GET /api/leaderboard?limit=10 */
    @GetMapping
    public ApiResponse<List<LeaderboardEntry>> leaderboard(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(leaderboardService.getTopLearners(Math.min(limit, 50)));
    }
}

// ─────────────────────────────────────────────────────────────
//  Platform Stats Controller (homepage counters)
// ─────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
class StatsController {

    @Autowired
    private StatsService statsService;

    /** GET /api/stats */
    @GetMapping
    public ApiResponse<PlatformStats> stats() {
        return ApiResponse.ok(statsService.getStats());
    }
}
