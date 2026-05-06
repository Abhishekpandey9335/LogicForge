package com.apcode.dto;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PlatformStats {
    private Long totalStudents;
    private Long totalVideos;
    private Long totalCourses;
    private Long totalSubscribers;
    private Double averageRating;
}
