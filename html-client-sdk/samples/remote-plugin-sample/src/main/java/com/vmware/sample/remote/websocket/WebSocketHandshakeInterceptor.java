/*
 * ******************************************************************
 * Copyright (c) 2020-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

package com.vmware.sample.remote.websocket;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;


public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
   private static final String TICKET_PARAMETER_NAME = "session-ticket";
   private final WebSocketSessionTicketService _webSocketSessionTicketService;

   public WebSocketHandshakeInterceptor(
         final WebSocketSessionTicketService webSocketSessionTicketService) {
      _webSocketSessionTicketService = webSocketSessionTicketService;
   }

   @Override
   public boolean beforeHandshake(
         final ServerHttpRequest request,
         final ServerHttpResponse response,
         final WebSocketHandler wsHandler,
         final Map<String, Object> attributes) {

      final HttpServletRequest httpServletRequest =
            ((ServletServerHttpRequest) request).getServletRequest();
      final HttpServletResponse httpServletResponse =
            ((ServletServerHttpResponse) response).getServletResponse();

      final String ticket = httpServletRequest.getParameter(TICKET_PARAMETER_NAME);
      if (!_webSocketSessionTicketService.validateTicket(ticket)) {
         httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
         return false;
      }

      return true;
   }

   @Override
   public void afterHandshake(final ServerHttpRequest request,
         final ServerHttpResponse response,
         final WebSocketHandler wsHandler,
         final Exception e) {
   }
}
