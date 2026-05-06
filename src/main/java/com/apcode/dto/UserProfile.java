package com.apcode.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class UserProfile {
    private Long id;
    private String fullName;
    private String email;
    private String city;
    private Integer totalPoints;
    private Integer currentStreakDays;
    private Boolean newsletterSubscribed;
    private LocalDateTime createdAt;
    private List<EnrollmentSummary> enrollments;
}
