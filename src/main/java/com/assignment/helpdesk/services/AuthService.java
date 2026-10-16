package com.assignment.helpdesk.services;

import com.assignment.helpdesk.dtos.request.LoginRequest;
import com.assignment.helpdesk.dtos.response.AuthResponse;
import com.assignment.helpdesk.dtos.response.CurrentUserResponse;
import com.assignment.helpdesk.entities.User;
import com.assignment.helpdesk.security.UserPrincipal;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    CurrentUserResponse getCurrentUser(UserPrincipal principal);

    AuthResponse buildAuthResponse(User user);
}
