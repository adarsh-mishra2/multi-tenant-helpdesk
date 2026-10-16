package com.assignment.helpdesk.dtos.response;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, CurrentUserResponse user) {

    public static AuthResponse bearer(String token, long expiresInSeconds, CurrentUserResponse user) {
        return new AuthResponse(token, "Bearer", expiresInSeconds, user);
    }
}
