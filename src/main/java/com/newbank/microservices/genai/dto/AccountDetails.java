package com.newbank.microservices.genai.dto;

import java.util.List;

public record AccountDetails(
    int id,
    String name,
    String birthDate,
    AccountType type,
    List<TransactionDetails> transactions
) {
}
