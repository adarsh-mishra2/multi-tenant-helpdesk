package com.assignment.helpdesk.controllers;

import com.assignment.helpdesk.dtos.request.CreateUserRequest;
import com.assignment.helpdesk.dtos.request.UpdateUserStatusRequest;
import com.assignment.helpdesk.dtos.response.PageResponse;
import com.assignment.helpdesk.dtos.response.UserResponse;
import com.assignment.helpdesk.entities.enums.Role;
import com.assignment.helpdesk.security.SecurityExpressions;
import com.assignment.helpdesk.security.UserPrincipal;
import com.assignment.helpdesk.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Tag(name = "Users", description = "User management inside the caller's organization")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "List users", description = "Agents use this to find assignees. Optional role filter.")
    @PreAuthorize(SecurityExpressions.STAFF)
    @GetMapping
    public PageResponse<UserResponse> list(@RequestParam(required = false) Role role,
                                           @ParameterObject @PageableDefault(size = 20, sort = "fullName") Pageable pageable,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        return userService.listUsers(role, pageable, principal);
    }

    @Operation(summary = "Create a user", description = "Org admins add agents, customers or other admins to their own organization.")
    @ApiResponse(responseCode = "201", description = "User created")
    @ApiResponse(responseCode = "409", description = "Email already used in this organization")
    @PreAuthorize(SecurityExpressions.ADMIN)
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        UserResponse created = userService.createUser(request, principal);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Activate or deactivate a user", description = "Deactivated users can no longer log in or use existing tokens.")
    @PreAuthorize(SecurityExpressions.ADMIN)
    @PatchMapping("/{id}/status")
    public UserResponse updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody UpdateUserStatusRequest request,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        return userService.updateStatus(id, request.active(), principal);
    }
}
