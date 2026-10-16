package com.assignment.helpdesk.utils;

import com.assignment.helpdesk.exceptions.BadRequestException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.stream.Collectors;

public final class PageableValidator {

    private PageableValidator() {
    }

    public static void requireSortableBy(Pageable pageable, Set<String> allowedProperties) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedProperties.contains(order.getProperty())) {
                String allowed = allowedProperties.stream().sorted().collect(Collectors.joining(", "));
                throw new BadRequestException(
                        "Sorting by '" + order.getProperty() + "' is not supported. Allowed fields: " + allowed);
            }
        }
    }
}
