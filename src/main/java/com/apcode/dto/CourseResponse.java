package com.apcode.dto;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CourseResponse {
    private Long id;
    private String slug;
    private String title;
    private String description;
    private String icon;
    private String badge;
    private Integer totalLectures;
    private Boolean isFree;
    private Long enrollmentCount;
    private Integer userProgressPercent;
    private Boolean isEnrolled;
}
