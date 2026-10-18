package com.newbank.microservices.genai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public record AccountRequest(int id,
                   @JsonFormat(pattern = "yyyy-MM-dd")
                   Date birthDate,
                   String name,
                   int typeId
) {
}
