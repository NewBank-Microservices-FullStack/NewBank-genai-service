package com.newbank.microservices.genai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** #3: constraints are enforced with @Valid at the tool/controller boundary. */
public record AccountRequest(int id,
                   @JsonFormat(pattern = "yyyy-MM-dd")
                   @NotNull Date birthDate,
                   @NotBlank String name,
                   @Min(1) int typeId
) {
}
