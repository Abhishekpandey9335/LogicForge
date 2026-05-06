package com.apcode.config;

import com.apcode.entity.*;
import com.apcode.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final CourseRepository courseRepo;
    private final VideoRepository videoRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdmin();
        seedCourses();
        seedVideos();
        log.info("✅ Data seeding complete");
    }

    private void seedAdmin() {
        if (userRepo.existsByEmail("admin@apcode.in")) return;
        User admin = User.builder()
                .fullName("Abhishek Pandey")
                .email("admin@apcode.in")
                .password(passwordEncoder.encode("Admin@2026"))
                .role(User.Role.ADMIN)
                .city("Varanasi")
                .totalPoints(9999)
                .currentStreakDays(365)
                .build();
        userRepo.save(admin);
        log.info("Admin seeded: admin@apcode.in / Admin@2026");
    }

    private void seedCourses() {
        if (courseRepo.existsBySlug("java")) return;

        List<Course> courses = List.of(
            Course.builder().slug("java").title("Java Programming (Beginner → Advanced)")
                .description("Learn Java from scratch with clear explanations, real examples, and interview-focused concepts.")
                .icon("☕").badge("Popular").isFree(true).totalLectures(25).build(),

            Course.builder().slug("dsa").title("Data Structures & Algorithms")
                .description("Master arrays, linked lists, stacks, queues, trees, and graphs with real interview problems.")
                .icon("🌳").badge("Free").isFree(true).totalLectures(40).build(),

            Course.builder().slug("web").title("Web Development (HTML, CSS, JS)")
                .description("Build responsive websites and real-world projects using modern web technologies.")
                .icon("🌐").badge(null).isFree(true).totalLectures(30).build(),

            Course.builder().slug("interview").title("Interview Prep & Placement")
                .description("Prepare for technical interviews with problem-solving strategies and company-wise questions.")
                .icon("🎯").badge(null).isFree(true).totalLectures(20).build()
        );

        courseRepo.saveAll(courses);
        log.info("Courses seeded: java, dsa, web, interview");
    }

    private void seedVideos() {
        if (videoRepo.count() > 0) return;

        Course javaCourse = courseRepo.findBySlug("java").orElseThrow();
        Course dsaCourse  = courseRepo.findBySlug("dsa").orElseThrow();

        List<Video> videos = List.of(
            Video.builder().youtubeId("9QJtEuaki1w").title("Complete Java in One Shot")
                .description("Start your coding journey!").isFree(true).orderIndex(1)
                .thumbnailUrl("https://i.ytimg.com/vi/9QJtEuaki1w/mqdefault.jpg")
                .course(javaCourse).build(),

            Video.builder().youtubeId("25JC2RIEFYY").title("Complete Array in One Shot")
                .description("Learn arrays visually.").isFree(true).orderIndex(1)
                .thumbnailUrl("https://i.ytimg.com/vi/25JC2RIEFYY/mqdefault.jpg")
                .course(dsaCourse).build(),

            Video.builder().youtubeId("xTkgfhM7cDk").title("Perfect Number — LeetCode")
                .description("Problem solving walkthrough.").isFree(true).orderIndex(2)
                .thumbnailUrl("https://i.ytimg.com/vi/xTkgfhM7cDk/mqdefault.jpg")
                .course(dsaCourse).build(),

            Video.builder().youtubeId("6FMTBYycXKA").title("Count Operations to Zero")
                .description("LeetCode problem explained.").isFree(true).orderIndex(3)
                .thumbnailUrl("https://i.ytimg.com/vi/6FMTBYycXKA/mqdefault.jpg")
                .course(dsaCourse).build(),

            Video.builder().youtubeId("OYY2qmPbLqw").title("Move Zeroes — LeetCode")
                .description("Two-pointer technique.").isFree(true).orderIndex(4)
                .thumbnailUrl("https://i.ytimg.com/vi/OYY2qmPbLqw/mqdefault.jpg")
                .course(dsaCourse).build()
        );

        videoRepo.saveAll(videos);
        log.info("Sample videos seeded");
    }
}
