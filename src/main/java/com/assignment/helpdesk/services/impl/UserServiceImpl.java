package com.assignment.helpdesk.services.impl;

import com.assignment.helpdesk.dtos.request.CreateUserRequest;
import com.assignment.helpdesk.dtos.response.PageResponse;
import com.assignment.helpdesk.dtos.response.UserResponse;
import com.assignment.helpdesk.entities.User;
import com.assignment.helpdesk.entities.enums.Role;
import com.assignment.helpdesk.exceptions.BadRequestException;
import com.assignment.helpdesk.exceptions.ConflictException;
import com.assignment.helpdesk.exceptions.ResourceNotFoundException;
import com.assignment.helpdesk.mappers.UserMapper;
import com.assignment.helpdesk.repositories.OrganizationRepository;
import com.assignment.helpdesk.repositories.UserRepository;
import com.assignment.helpdesk.security.UserPrincipal;
import com.assignment.helpdesk.services.UserService;
import com.assignment.helpdesk.utils.PageableValidator;
import com.assignment.helpdesk.utils.TextNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "fullName", "email", "role", "createdAt");

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request, UserPrincipal actor) {
        String email = TextNormalizer.email(request.email());
        if (userRepository.existsByEmailInOrganization(actor.getOrganizationId(), email)) {
            throw new ConflictException("A user with email " + email + " already exists in your organization");
        }

        User user = new User();
        user.setOrganization(organizationRepository.getReferenceById(actor.getOrganizationId()));
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        userRepository.save(user);

        log.info("User {} created with role {} in organization {} by {}",
                user.getId(), user.getRole(), actor.getOrganizationId(), actor.getUserId());
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(Role role, Pageable pageable, UserPrincipal actor) {
        PageableValidator.requireSortableBy(pageable, SORTABLE_FIELDS);
        Page<User> users = role == null
                ? userRepository.findAllInOrganization(actor.getOrganizationId(), pageable)
                : userRepository.findAllInOrganizationByRole(actor.getOrganizationId(), role, pageable);
        return PageResponse.from(users.map(userMapper::toResponse));
    }

    @Override
    @Transactional
    public UserResponse updateStatus(Long userId, boolean active, UserPrincipal actor) {
        if (userId.equals(actor.getUserId()) && !active) {
            throw new BadRequestException("You cannot deactivate your own account");
        }
        User user = userRepository.findByIdAndOrganizationId(userId, actor.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setActive(active);
        userRepository.saveAndFlush(user);
        return userMapper.toResponse(user);
    }
}
