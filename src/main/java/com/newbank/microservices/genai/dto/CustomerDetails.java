package com.newbank.microservices.genai.dto;

import java.util.List;

public record CustomerDetails(
    int id,
    String firstName,
    String lastName,
    String address,
    String city,
    String telephone,
    List<AccountDetails> accounts
) {
}
