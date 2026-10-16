package com.assignment.helpdesk.security;

public final class SecurityExpressions {

    public static final String ADMIN = "hasRole('ORG_ADMIN')";
    public static final String STAFF = "hasAnyRole('ORG_ADMIN', 'AGENT')";

    private SecurityExpressions() {
    }
}
