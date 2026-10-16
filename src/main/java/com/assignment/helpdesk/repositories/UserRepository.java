package com.assignment.helpdesk.repositories;

import com.assignment.helpdesk.entities.User;
import com.assignment.helpdesk.entities.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select u from User u join fetch u.organization o where o.slug = :slug and u.email = :email")
    Optional<User> findForLogin(@Param("slug") String organizationSlug, @Param("email") String email);

    @Query("select u from User u join fetch u.organization where u.id = :id")
    Optional<User> findWithOrganizationById(@Param("id") Long id);

    @Query("select u from User u join fetch u.organization where u.id = :id and u.organization.id = :organizationId")
    Optional<User> findByIdAndOrganizationId(@Param("id") Long id, @Param("organizationId") Long organizationId);

    @Query("""
            select case when count(u) > 0 then true else false end
            from User u
            where u.organization.id = :organizationId and u.email = :email
            """)
    boolean existsByEmailInOrganization(@Param("organizationId") Long organizationId, @Param("email") String email);

    @Query("select u from User u where u.organization.id = :organizationId")
    Page<User> findAllInOrganization(@Param("organizationId") Long organizationId, Pageable pageable);

    @Query("select u from User u where u.organization.id = :organizationId and u.role = :role")
    Page<User> findAllInOrganizationByRole(@Param("organizationId") Long organizationId,
                                           @Param("role") Role role,
                                           Pageable pageable);
}
