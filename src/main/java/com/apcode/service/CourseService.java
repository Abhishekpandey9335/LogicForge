package com.apcode.service;

import com.apcode.dto.*;
import com.apcode.entity.*;
import com.apcode.exception.*;
import com.apcode.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    // ── Public: list all published courses ───────────────────
    @Transactional(readOnly = true)
    public List<CourseResponse> getAllCourses(String currentUserEmail) {
        Long userId = resolveUserId(currentUserEmail);
        return courseRepository.findByIsPublishedTrueOrderByIdAsc()
                .stream()
                .map(c -> mapCourse(c, userId))
                .collect(Collectors.toList());
    }

    // ── Public: get course by slug ────────────────────────────
    @Transactional(readOnly = true)
    public CourseResponse getCourseBySlug(String slug, String currentUserEmail) {
        Course course = courseRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + slug));
        Long userId = resolveUserId(currentUserEmail);
        return mapCourse(course, userId);
    }

    // ── Admin: create a course ────────────────────────────────
    @Transactional
    public CourseResponse createCourse(CreateCourseRequest request) {
        if (courseRepository.existsBySlug(request.getSlug())) {
            throw new BadRequestException("Course slug already exists: " + request.getSlug());
        }
        Course course = Course.builder()
                .slug(request.getSlug())
                .title(request.getTitle())
                .description(request.getDescription())
                .icon(request.getIcon())
                .badge(request.getBadge())
                .isFree(request.getIsFree())
                .build();
        return mapCourse(courseRepository.save(course), null);
    }

    // ── Enroll authenticated user in a course ─────────────────
    @Transactional
    public EnrollmentSummary enrollUser(String courseSlug, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Course course = courseRepository.findBySlug(courseSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseSlug));

        if (enrollmentRepository.existsByUserIdAndCourseId(user.getId(), course.getId())) {
            throw new BadRequestException("Already enrolled in this course");
        }

        CourseEnrollment enrollment = CourseEnrollment.builder()
                .user(user)
                .course(course)
                .progressPercent(0)
                .build();

        enrollment = enrollmentRepository.save(enrollment);

        // Award 50 points for enrolling
        user.setTotalPoints(user.getTotalPoints() + 50);
        userRepository.save(user);

        return mapEnrollment(enrollment);
    }

    // ── Mark a video as complete ──────────────────────────────
    @Transactional
    public EnrollmentSummary markVideoComplete(String courseSlug, Long videoId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CourseEnrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseSlug(user.getId(), courseSlug)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Not enrolled in course: " + courseSlug));

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found: " + videoId));

        if (!enrollment.getCompletedVideoIds().contains(videoId)) {
            enrollment.getCompletedVideoIds().add(videoId);

            // Recalculate progress
            long total = videoRepository.countByCourseId(enrollment.getCourse().getId());
            int completed = enrollment.getCompletedVideoIds().size();
            int percent = total > 0 ? (int) Math.round((completed * 100.0) / total) : 0;
            enrollment.setProgressPercent(percent);

            // Award 10 points per completed lecture
            user.setTotalPoints(user.getTotalPoints() + 10);
            userRepository.save(user);
            enrollmentRepository.save(enrollment);
        }

        return mapEnrollment(enrollment);
    }

    // ── Get user enrollments ──────────────────────────────────
    @Transactional(readOnly = true)
    public List<EnrollmentSummary> getUserEnrollments(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return enrollmentRepository.findByUserId(user.getId())
                .stream().map(this::mapEnrollment).collect(Collectors.toList());
    }

    // ── Mappers ───────────────────────────────────────────────
    private CourseResponse mapCourse(Course course, Long userId) {
        long enrollmentCount = enrollmentRepository.countEnrollmentsByCourseId(course.getId());
        Integer progressPercent = null;
        Boolean isEnrolled = false;

        if (userId != null) {
            var enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, course.getId());
            if (enrollment.isPresent()) {
                isEnrolled = true;
                progressPercent = enrollment.get().getProgressPercent();
            }
        }

        return CourseResponse.builder()
                .id(course.getId())
                .slug(course.getSlug())
                .title(course.getTitle())
                .description(course.getDescription())
                .icon(course.getIcon())
                .badge(course.getBadge())
                .totalLectures(course.getTotalLectures())
                .isFree(course.getIsFree())
                .enrollmentCount(enrollmentCount)
                .userProgressPercent(progressPercent)
                .isEnrolled(isEnrolled)
                .build();
    }

    private EnrollmentSummary mapEnrollment(CourseEnrollment e) {
        return EnrollmentSummary.builder()
                .enrollmentId(e.getId())
                .courseSlug(e.getCourse().getSlug())
                .courseTitle(e.getCourse().getTitle())
                .progressPercent(e.getProgressPercent())
                .enrolledAt(e.getEnrolledAt())
                .lastAccessedAt(e.getLastAccessedAt())
                .build();
    }

    private Long resolveUserId(String email) {
        if (email == null || email.equals("anonymousUser")) return null;
        return userRepository.findByEmail(email).map(User::getId).orElse(null);
    }
}
