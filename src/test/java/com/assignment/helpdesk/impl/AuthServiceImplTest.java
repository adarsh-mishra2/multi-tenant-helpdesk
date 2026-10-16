package com.assignment.helpdesk.impl;

import com.assignment.helpdesk.dtos.request.LoginRequest;
import com.assignment.helpdesk.dtos.response.AuthResponse;
import com.assignment.helpdesk.entities.User;
import com.assignment.helpdesk.entities.enums.Role;
import com.assignment.helpdesk.mappers.UserMapper;
import com.assignment.helpdesk.repositories.UserRepository;
import com.assignment.helpdesk.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static com.assignment.helpdesk.support.TestFixtures.organization;
import static com.assignment.helpdesk.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthServiceImpl authService;
    private User agent;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, jwtService, new UserMapper());
        agent = user(2L, organization(1L, "acme"), Role.AGENT, "Rahul Verma");
    }

    @Test
    void validCredentialsReturnBearerToken() {
        when(userRepository.findForLogin("acme", "agent1@acme.com")).thenReturn(Optional.of(agent));
        when(passwordEncoder.matches("Password@123", agent.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(2L, 1L, Role.AGENT)).thenReturn("signed.jwt.token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(new LoginRequest(" ACME ", "Agent1@acme.com", "Password@123"));

        assertThat(response.accessToken()).isEqualTo("signed.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().organizationSlug()).isEqualTo("acme");
    }

    @Test
    void wrongPasswordIsRejected() {
        when(userRepository.findForLogin("acme", "agent1@acme.com")).thenReturn(Optional.of(agent));
        when(passwordEncoder.matches("wrong", agent.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("acme", "agent1@acme.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage(AuthServiceImpl.INVALID_CREDENTIALS);
    }

    @Test
    void unknownUserGetsSameErrorAndStillPaysBcryptCost() {
        when(userRepository.findForLogin("acme", "ghost@acme.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("acme", "ghost@acme.com", "whatever")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage(AuthServiceImpl.INVALID_CREDENTIALS);
        verify(passwordEncoder).matches(anyString(), anyString());
    }

    @Test
    void deactivatedUserCannotLogIn() {
        agent.setActive(false);
        when(userRepository.findForLogin("acme", "agent1@acme.com")).thenReturn(Optional.of(agent));
        when(passwordEncoder.matches("Password@123", agent.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("acme", "agent1@acme.com", "Password@123")))
                .isInstanceOf(DisabledException.class);
    }
}
