package com.assignment.helpdesk.mappers;

import com.assignment.helpdesk.dtos.response.CurrentUserResponse;
import com.assignment.helpdesk.dtos.response.UserResponse;
import com.assignment.helpdesk.dtos.response.UserSummaryResponse;
import com.assignment.helpdesk.entities.Organization;
import com.assignment.helpdesk.entities.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserSummaryResponse toSummary(User user) {
        if (user == null) {
            return null;
        }
        return new UserSummaryResponse(user.getId(), user.getFullName(), user.getRole());
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt());
    }

    public CurrentUserResponse toCurrentUser(User user) {
        Organization organization = user.getOrganization();
        return new CurrentUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                organization.getId(),
                organization.getName(),
                organization.getSlug());
    }
}
