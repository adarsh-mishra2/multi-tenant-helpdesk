package com.assignment.helpdesk.services.impl;

import com.assignment.helpdesk.dtos.request.RegisterOrganizationRequest;
import com.assignment.helpdesk.dtos.response.AuthResponse;
import com.assignment.helpdesk.dtos.response.OrganizationResponse;
import com.assignment.helpdesk.entities.Organization;
import com.assignment.helpdesk.entities.User;
import com.assignment.helpdesk.entities.enums.Role;
import com.assignment.helpdesk.exceptions.ConflictException;
import com.assignment.helpdesk.exceptions.ResourceNotFoundException;
import com.assignment.helpdesk.mappers.OrganizationMapper;
import com.assignment.helpdesk.repositories.OrganizationRepository;
import com.assignment.helpdesk.repositories.UserRepository;
import com.assignment.helpdesk.security.UserPrincipal;
import com.assignment.helpdesk.services.AuthService;
import com.assignment.helpdesk.services.OrganizationService;
import com.assignment.helpdesk.utils.TextNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrganizationMapper organizationMapper;
    private final AuthService authService;

    @Override
    @Transactional
    public AuthResponse register(RegisterOrganizationRequest request) {
        String slug = TextNormalizer.slug(request.organizationSlug());
        if (organizationRepository.existsBySlug(slug)) {
            throw new ConflictException("The organization slug '" + slug + "' is already taken");
        }

        Organization organization = new Organization(request.organizationName().trim(), slug);
        organizationRepository.save(organization);

        User admin = new User();
        admin.setOrganization(organization);
        admin.setFullName(request.adminFullName().trim());
        admin.setEmail(TextNormalizer.email(request.adminEmail()));
        admin.setPasswordHash(passwordEncoder.encode(request.adminPassword()));
        admin.setRole(Role.ORG_ADMIN);
        userRepository.save(admin);

        log.info("Registered organization {} ({})", organization.getId(), slug);
        return authService.buildAuthResponse(admin);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse getCurrentOrganization(UserPrincipal principal) {
        return organizationRepository.findById(principal.getOrganizationId())
                .map(organizationMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", principal.getOrganizationId()));
    }
}
