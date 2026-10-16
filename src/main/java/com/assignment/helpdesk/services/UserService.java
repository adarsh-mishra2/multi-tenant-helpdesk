package com.assignment.helpdesk.services;

import com.assignment.helpdesk.dtos.request.CreateUserRequest;
import com.assignment.helpdesk.dtos.response.PageResponse;
import com.assignment.helpdesk.dtos.response.UserResponse;
import com.assignment.helpdesk.entities.enums.Role;
import com.assignment.helpdesk.security.UserPrincipal;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse createUser(CreateUserRequest request, UserPrincipal actor);

    PageResponse<UserResponse> listUsers(Role role, Pageable pageable, UserPrincipal actor);

    UserResponse updateStatus(Long userId, boolean active, UserPrincipal actor);
}
