package com.assignment.helpdesk.support;

import com.assignment.helpdesk.entities.Organization;
import com.assignment.helpdesk.entities.Ticket;
import com.assignment.helpdesk.entities.User;
import com.assignment.helpdesk.entities.enums.Role;
import com.assignment.helpdesk.entities.enums.TicketPriority;
import com.assignment.helpdesk.entities.enums.TicketStatus;
import com.assignment.helpdesk.security.UserPrincipal;
import org.springframework.test.util.ReflectionTestUtils;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static Organization organization(long id, String slug) {
        Organization organization = new Organization(slug.toUpperCase() + " Inc", slug);
        ReflectionTestUtils.setField(organization, "id", id);
        return organization;
    }

    public static User user(long id, Organization organization, Role role, String fullName) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setOrganization(organization);
        user.setRole(role);
        user.setFullName(fullName);
        user.setEmail(fullName.toLowerCase().replace(' ', '.') + "@" + organization.getSlug() + ".com");
        user.setPasswordHash("$2a$10$hash");
        user.setActive(true);
        return user;
    }

    public static Ticket ticket(long id, Organization organization, User requester, TicketStatus status) {
        Ticket ticket = new Ticket();
        ReflectionTestUtils.setField(ticket, "id", id);
        ticket.setOrganization(organization);
        ticket.setRequester(requester);
        ticket.setSubject("Printer is offline");
        ticket.setDescription("The office printer shows offline since morning.");
        ticket.setPriority(TicketPriority.MEDIUM);
        ticket.setStatus(status);
        return ticket;
    }

    public static UserPrincipal principal(User user) {
        return UserPrincipal.from(user);
    }
}
