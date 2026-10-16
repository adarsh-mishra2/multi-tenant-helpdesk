package com.assignment.helpdesk.services;

import com.assignment.helpdesk.dtos.request.RegisterOrganizationRequest;
import com.assignment.helpdesk.dtos.response.AuthResponse;
import com.assignment.helpdesk.dtos.response.OrganizationResponse;
import com.assignment.helpdesk.security.UserPrincipal;

public interface OrganizationService {

    AuthResponse register(RegisterOrganizationRequest request);

    OrganizationResponse getCurrentOrganization(UserPrincipal principal);
}
