package com.assignment.helpdesk.services;

import com.assignment.helpdesk.dtos.request.CreateTicketRequest;
import com.assignment.helpdesk.dtos.request.TicketSearchCriteria;
import com.assignment.helpdesk.dtos.request.UpdateTicketRequest;
import com.assignment.helpdesk.dtos.response.PageResponse;
import com.assignment.helpdesk.dtos.response.TicketHistoryResponse;
import com.assignment.helpdesk.dtos.response.TicketResponse;
import com.assignment.helpdesk.dtos.response.TicketSummaryResponse;
import com.assignment.helpdesk.entities.enums.TicketStatus;
import com.assignment.helpdesk.security.UserPrincipal;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TicketService {

    TicketResponse createTicket(CreateTicketRequest request, UserPrincipal actor);

    PageResponse<TicketSummaryResponse> searchTickets(TicketSearchCriteria criteria, Pageable pageable, UserPrincipal actor);

    TicketResponse getTicket(Long ticketId, UserPrincipal actor);

    TicketResponse updateTicket(Long ticketId, UpdateTicketRequest request, UserPrincipal actor);

    TicketResponse changeStatus(Long ticketId, TicketStatus target, UserPrincipal actor);

    TicketResponse assignTicket(Long ticketId, Long assigneeId, UserPrincipal actor);

    List<TicketHistoryResponse> getHistory(Long ticketId, UserPrincipal actor);
}
