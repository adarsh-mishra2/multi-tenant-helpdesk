package com.assignment.helpdesk.repositories;

import com.assignment.helpdesk.entities.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    @Query("""
            select t from Ticket t
            join fetch t.requester
            left join fetch t.assignee
            where t.id = :id and t.organization.id = :organizationId
            """)
    Optional<Ticket> findDetailedByIdAndOrganizationId(@Param("id") Long id,
                                                       @Param("organizationId") Long organizationId);

    @Override
    @EntityGraph(attributePaths = {"requester", "assignee"})
    Page<Ticket> findAll(Specification<Ticket> spec, Pageable pageable);
}
