/*
 * ******************************************************************
 * Copyright (c) 2022-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote.controllers;

import com.vmware.sample.remote.websocket.WebSocketSessionTicketService;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * A controller to serve for managing web socket session.
 */
@RestController
@RequestMapping("/rest/websocket/session")
public class WebSocketSessionController {

   private final WebSocketSessionTicketService _webSocketSessionTicketService;

   public WebSocketSessionController(final WebSocketSessionTicketService webSocketSessionTicketService) {
      _webSocketSessionTicketService = webSocketSessionTicketService;
   }

   /**
    * Generate a web socket session ticket for the current session. The current session
    * is authenticated by the {@link com.vmware.sample.remote.filters.SecurityFilter}.
    *
    * @return a newly generated web socket session ticket.
    */
   @RequestMapping(value = "/generate-ticket", method = RequestMethod.GET)
   public String generateTicket() {
      return _webSocketSessionTicketService.generateTicket();
   }
}
