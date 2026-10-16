package com.assignment.helpdesk.controllers;

import com.assignment.helpdesk.dtos.request.LoginRequest;
import com.assignment.helpdesk.dtos.response.AuthResponse;
import com.assignment.helpdesk.dtos.response.CurrentUserResponse;
import com.assignment.helpdesk.security.UserPrincipal;
import com.assignment.helpdesk.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Log in", description = "Returns a JWT access token for the given organization, email and password.")
    @ApiResponse(responseCode = "200", description = "Authenticated")
    @ApiResponse(responseCode = "401", description = "Invalid credentials or deactivated account")
    @SecurityRequirements
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Current user", description = "Profile and organization of the authenticated caller.")
    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return authService.getCurrentUser(principal);
    }
}
