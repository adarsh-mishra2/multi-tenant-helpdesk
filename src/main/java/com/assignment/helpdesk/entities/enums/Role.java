package com.assignment.helpdesk.entities.enums;

public enum Role {
    ORG_ADMIN,
    AGENT,
    CUSTOMER;

    public boolean isStaff() {
        return this != CUSTOMER;
    }

    public String authority() {
        return "ROLE_" + name();
    }
}
