package com.newbank.microservices.genai.dto;

import java.util.Set;

public record Advisor(
    Integer id,
    String firstName,
    String lastName,
    Set<Expertise> specialties) {
}
