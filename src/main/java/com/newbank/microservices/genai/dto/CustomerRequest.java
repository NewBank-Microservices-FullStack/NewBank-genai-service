package com.newbank.microservices.genai.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;

/** #3: constraints are enforced with @Valid at the tool/controller boundary. */
public record CustomerRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String address,
        @NotBlank String city,
        @NotBlank @Digits(fraction = 0, integer = 12) String telephone) {
}
