package com.apcode.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class CreateVideoRequest {
    @NotBlank private String youtubeId;
    @NotBlank private String title;
    private String description;
    private String thumbnailUrl;
    private Integer orderIndex = 0;
    private Boolean isFree = true;
    @NotNull private Long courseId;
}
