package com.assignment.helpdesk.mappers;

import com.assignment.helpdesk.dtos.response.OrganizationResponse;
import com.assignment.helpdesk.entities.Organization;
import org.springframework.stereotype.Component;

@Component
public class OrganizationMapper {

    public OrganizationResponse toResponse(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                organization.getCreatedAt());
    }
}
