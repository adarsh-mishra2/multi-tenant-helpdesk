package com.assignment.helpdesk.services.impl;

import com.assignment.helpdesk.dtos.request.LoginRequest;
import com.assignment.helpdesk.dtos.response.AuthResponse;
import com.assignment.helpdesk.dtos.response.CurrentUserResponse;
import com.assignment.helpdesk.entities.User;
import com.assignment.helpdesk.exceptions.ResourceNotFoundException;
import com.assignment.helpdesk.mappers.UserMapper;
import com.assignment.helpdesk.repositories.UserRepository;
import com.assignment.helpdesk.security.JwtService;
import com.assignment.helpdesk.security.UserPrincipal;
import com.assignment.helpdesk.services.AuthService;
import com.assignment.helpdesk.utils.TextNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    static final String INVALID_CREDENTIALS = "Invalid organization, email or password";

    private static final String DUMMY_HASH = "$2a$10$VheKAOnUoL0OL7MoRPryNe2LPm8duZUQRgWEmSZM8yTqNT420gh6C";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Optional<User> candidate = userRepository.findForLogin(
                TextNormalizer.slug(request.organizationSlug()),
                TextNormalizer.email(request.email()));

        String hash = candidate.map(User::getPasswordHash).orElse(DUMMY_HASH);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);

        User user = candidate
                .filter(found -> passwordMatches)
                .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS));

        if (!user.isActive()) {
            throw new DisabledException("This account has been deactivated. Contact your organization admin.");
        }

        log.info("User {} logged in to organization {}", user.getId(), user.getOrganization().getId());
        return buildAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(UserPrincipal principal) {
        User user = userRepository.findWithOrganizationById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getUserId()));
        return userMapper.toCurrentUser(user);
    }

    @Override
    public AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user.getId(), user.getOrganization().getId(), user.getRole());
        return AuthResponse.bearer(token, jwtService.getExpirationSeconds(), userMapper.toCurrentUser(user));
    }
}
