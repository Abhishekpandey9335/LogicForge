package com.apcode.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class MarkCompleteRequest {
    @NotNull private Long videoId;
}
