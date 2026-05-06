package com.apcode.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class CreateCourseRequest {
    @NotBlank private String slug;
    @NotBlank private String title;
    private String description;
    private String icon;
    private String badge;
    private Boolean isFree = true;
}
