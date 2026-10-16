package com.assignment.helpdesk.controllers;

import com.assignment.helpdesk.dtos.request.RegisterOrganizationRequest;
import com.assignment.helpdesk.dtos.response.AuthResponse;
import com.assignment.helpdesk.dtos.response.OrganizationResponse;
import com.assignment.helpdesk.security.UserPrincipal;
import com.assignment.helpdesk.services.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Tag(name = "Organizations")
@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @Operation(summary = "Register an organization",
            description = "Public signup. Creates a new tenant with its first ORG_ADMIN and returns that admin's token.")
    @ApiResponse(responseCode = "201", description = "Organization created")
    @ApiResponse(responseCode = "409", description = "Slug already taken")
    @SecurityRequirements
    @PostMapping
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterOrganizationRequest request) {
        AuthResponse response = organizationService.register(request);
        return ResponseEntity.created(URI.create("/api/v1/organizations/current")).body(response);
    }

    @Operation(summary = "Caller's organization")
    @GetMapping("/current")
    public OrganizationResponse current(@AuthenticationPrincipal UserPrincipal principal) {
        return organizationService.getCurrentOrganization(principal);
    }
}
