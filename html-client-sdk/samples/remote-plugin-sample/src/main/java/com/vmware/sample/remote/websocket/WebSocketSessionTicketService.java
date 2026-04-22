/*
 * ******************************************************************
 * Copyright (c) 2022-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote.websocket;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Cache;

/**
 * Service responsible for generating and storing websocket session tickets.
 * One such ticket can be used to create a websocket session from the frontend code.
 * This is required since the WebSocket API used in this sample does not support
 * sending http headers during the initialization of the websocket. This is problematic
 * since the PSID is required for the authentication of the client. This way
 * a less sensitive session ticket is generated based on the PSID which is later used
 * to initialize the WebSocket.
 */
public class WebSocketSessionTicketService {
   private static final Log logger = LogFactory
         .getLog(WebSocketSessionTicketService.class);

   static final byte SESSION_TICKET_LENGTH_BYTES = 32;
   /**
    * Random generator instance used by this class to generate random bytes.
    */
   private static final SecureRandom RANDOM = new SecureRandom();

   private final Cache<String, String> sessionTicketCache = CacheBuilder
         .newBuilder().maximumSize(100).expireAfterWrite(1, TimeUnit.MINUTES)
         .build();

   public String generateTicket() {
      final String ticket = generateSecureToken();
      sessionTicketCache.put(ticket, ticket);
      return ticket;
   }

   public boolean validateTicket(final String ticket) {
      final boolean isValid = sessionTicketCache.getIfPresent(ticket) != null;
      sessionTicketCache.invalidate(ticket);
      return isValid;
   }

    private String generateSecureToken() {
       final byte[] randomBytes = new byte[SESSION_TICKET_LENGTH_BYTES];
       RANDOM.nextBytes(randomBytes);
       return Base64.getEncoder().encodeToString(randomBytes);
    }
}
