package com.agrodata.dto;

public record LoginResponseDTO(String accessToken, String tokenType, long expiresIn, String username, String role, boolean senhaTemporaria) {}
