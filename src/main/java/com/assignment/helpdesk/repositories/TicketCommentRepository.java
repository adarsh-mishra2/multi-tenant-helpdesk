package com.assignment.helpdesk.repositories;

import com.assignment.helpdesk.entities.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {

    @Query("""
            select c from TicketComment c
            join fetch c.author
            where c.ticket.id = :ticketId
            order by c.createdAt asc, c.id asc
            """)
    List<TicketComment> findAllByTicketId(@Param("ticketId") Long ticketId);

    @Query("""
            select c from TicketComment c
            join fetch c.author
            where c.ticket.id = :ticketId and c.internal = false
            order by c.createdAt asc, c.id asc
            """)
    List<TicketComment> findPublicByTicketId(@Param("ticketId") Long ticketId);
}
