package com.assignment.helpdesk.services;

import com.assignment.helpdesk.dtos.request.CreateCommentRequest;
import com.assignment.helpdesk.dtos.response.CommentResponse;
import com.assignment.helpdesk.security.UserPrincipal;

import java.util.List;

public interface TicketCommentService {

    CommentResponse addComment(Long ticketId, CreateCommentRequest request, UserPrincipal actor);

    List<CommentResponse> getComments(Long ticketId, UserPrincipal actor);
}
