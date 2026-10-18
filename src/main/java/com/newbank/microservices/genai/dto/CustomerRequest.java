package com.newbank.microservices.genai.dto;

public record CustomerRequest(String firstName,
        String lastName,
        String address,
        String city,
        String telephone) {
}
