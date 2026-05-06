package com.apcode.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EnrollmentSummary {
    private Long enrollmentId;
    private String courseSlug;
    private String courseTitle;
    private Integer progressPercent;
    private LocalDateTime enrolledAt;
    private LocalDateTime lastAccessedAt;
}
