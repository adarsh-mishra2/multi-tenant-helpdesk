package com.assignment.helpdesk.repositories;

import com.assignment.helpdesk.entities.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    boolean existsBySlug(String slug);
}
