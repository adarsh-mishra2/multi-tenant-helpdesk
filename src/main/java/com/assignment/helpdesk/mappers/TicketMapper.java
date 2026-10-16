package com.assignment.helpdesk.mappers;

import com.assignment.helpdesk.dtos.response.CommentResponse;
import com.assignment.helpdesk.dtos.response.TicketHistoryResponse;
import com.assignment.helpdesk.dtos.response.TicketResponse;
import com.assignment.helpdesk.dtos.response.TicketSummaryResponse;
import com.assignment.helpdesk.entities.Ticket;
import com.assignment.helpdesk.entities.TicketComment;
import com.assignment.helpdesk.entities.TicketHistory;
import com.assignment.helpdesk.entities.enums.TicketStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TicketMapper {

    private final UserMapper userMapper;

    public TicketSummaryResponse toSummary(Ticket ticket) {
        return new TicketSummaryResponse(
                ticket.getId(),
                ticket.getSubject(),
                ticket.getPriority(),
                ticket.getStatus(),
                userMapper.toSummary(ticket.getRequester()),
                userMapper.toSummary(ticket.getAssignee()),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }

    public TicketResponse toResponse(Ticket ticket,
                                     List<TicketStatus> allowedTransitions,
                                     boolean editable,
                                     boolean assignable,
                                     boolean canAddInternalNotes) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getSubject(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getStatus(),
                userMapper.toSummary(ticket.getRequester()),
                userMapper.toSummary(ticket.getAssignee()),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getClosedAt(),
                allowedTransitions,
                editable,
                assignable,
                canAddInternalNotes);
    }

    public CommentResponse toComment(TicketComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getBody(),
                comment.isInternal(),
                userMapper.toSummary(comment.getAuthor()),
                comment.getCreatedAt());
    }

    public TicketHistoryResponse toHistory(TicketHistory history) {
        return new TicketHistoryResponse(
                history.getId(),
                history.getAction(),
                history.getFromValue(),
                history.getToValue(),
                userMapper.toSummary(history.getChangedBy()),
                history.getCreatedAt());
    }
}
