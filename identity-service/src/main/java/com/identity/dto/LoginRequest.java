package com.identity.dto;

public record LoginRequest(
    String username,
    String password
) {}
