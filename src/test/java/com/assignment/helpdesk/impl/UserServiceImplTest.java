package com.assignment.helpdesk.impl;

import com.assignment.helpdesk.dtos.request.CreateUserRequest;
import com.assignment.helpdesk.dtos.response.UserResponse;
import com.assignment.helpdesk.entities.Organization;
import com.assignment.helpdesk.entities.User;
import com.assignment.helpdesk.entities.enums.Role;
import com.assignment.helpdesk.exceptions.BadRequestException;
import com.assignment.helpdesk.exceptions.ConflictException;
import com.assignment.helpdesk.exceptions.ResourceNotFoundException;
import com.assignment.helpdesk.mappers.UserMapper;
import com.assignment.helpdesk.repositories.OrganizationRepository;
import com.assignment.helpdesk.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static com.assignment.helpdesk.support.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl userService;
    private Organization acme;
    private User admin;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, organizationRepository, passwordEncoder, new UserMapper());
        acme = organization(1L, "acme");
        admin = user(1L, acme, Role.ORG_ADMIN, "Asha Mehta");
    }

    @Test
    void createsUserInAdminsOrganizationWithNormalizedEmailAndHashedPassword() {
        when(userRepository.existsByEmailInOrganization(acme.getId(), "vikram@acme.com")).thenReturn(false);
        when(organizationRepository.getReferenceById(acme.getId())).thenReturn(acme);
        when(passwordEncoder.encode("Password@123")).thenReturn("hashed-value");

        UserResponse response = userService.createUser(
                new CreateUserRequest("Vikram Rao", "  Vikram@ACME.com ", "Password@123", Role.AGENT),
                principal(admin));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getOrganization()).isSameAs(acme);
        assertThat(saved.getValue().getEmail()).isEqualTo("vikram@acme.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed-value");
        assertThat(response.role()).isEqualTo(Role.AGENT);
    }

    @Test
    void duplicateEmailInSameOrganizationIsConflict() {
        when(userRepository.existsByEmailInOrganization(acme.getId(), "agent1@acme.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(
                new CreateUserRequest("Someone", "agent1@acme.com", "Password@123", Role.AGENT), principal(admin)))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void adminCannotDeactivateThemselves() {
        assertThatThrownBy(() -> userService.updateStatus(admin.getId(), false, principal(admin)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void cannotChangeStatusOfUserFromAnotherOrganization() {
        when(userRepository.findByIdAndOrganizationId(99L, acme.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateStatus(99L, false, principal(admin)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsUnknownSortField() {
        PageRequest pageable = PageRequest.of(0, 20, Sort.by("passwordHash"));

        assertThatThrownBy(() -> userService.listUsers(null, pageable, principal(admin)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("passwordHash");
    }
}
