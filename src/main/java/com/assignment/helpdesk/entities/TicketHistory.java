package com.assignment.helpdesk.entities;

import com.assignment.helpdesk.entities.enums.HistoryAction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "ticket_history")
public class TicketHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30, updatable = false)
    private HistoryAction action;

    @Column(name = "from_value", length = 200, updatable = false)
    private String fromValue;

    @Column(name = "to_value", length = 200, updatable = false)
    private String toValue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by_id", nullable = false, updatable = false)
    private User changedBy;

    private TicketHistory(Ticket ticket, HistoryAction action, String fromValue, String toValue, User changedBy) {
        this.ticket = ticket;
        this.action = action;
        this.fromValue = fromValue;
        this.toValue = toValue;
        this.changedBy = changedBy;
    }

    public static TicketHistory of(Ticket ticket, HistoryAction action, String fromValue, String toValue, User changedBy) {
        return new TicketHistory(ticket, action, fromValue, toValue, changedBy);
    }
}
