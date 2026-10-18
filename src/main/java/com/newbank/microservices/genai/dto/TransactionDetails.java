package com.newbank.microservices.genai.dto;

public record TransactionDetails(
    Integer id,
    Integer accountId,
    String date,
    String description) {
}
