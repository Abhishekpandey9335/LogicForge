package com.apcode.dto;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class VideoResponse {
    private Long id;
    private String youtubeId;
    private String title;
    private String description;
    private String thumbnailUrl;
    private Integer orderIndex;
    private Boolean isFree;
    private String courseSlug;
    private Boolean isCompleted;
}
