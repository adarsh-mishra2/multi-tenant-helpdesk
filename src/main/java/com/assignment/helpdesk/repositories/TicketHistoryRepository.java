package com.assignment.helpdesk.repositories;

import com.assignment.helpdesk.entities.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {

    @Query("""
            select h from TicketHistory h
            join fetch h.changedBy
            where h.ticket.id = :ticketId
            order by h.createdAt asc, h.id asc
            """)
    List<TicketHistory> findAllByTicketId(@Param("ticketId") Long ticketId);
}
