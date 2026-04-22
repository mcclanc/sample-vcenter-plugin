/*
 * ******************************************************************
 * Copyright (c) 2020-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

package com.vmware.sample.remote.services;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.sample.remote.model.Message;
import com.vmware.sample.remote.websocket.WebSocketSessionRegistry;

/**
 * Implementation of MessagingService.
 */
public class MessagingServiceImpl implements MessagingService {

   private static final Log logger = LogFactory.getLog(
         MessagingServiceImpl.class);

   private final WebSocketSessionRegistry registry;
   private final ObjectMapper objectMapper = new ObjectMapper();

   public MessagingServiceImpl(
         final WebSocketSessionRegistry registry) {
      this.registry = registry;
   }

   @Override
   public void broadcastMessage(final Message message) {
      final String serializedMessage;
      try {
         serializedMessage = objectMapper.writeValueAsString(message);
      } catch (Exception e) {
         logger.error("Failed to serialize message", e);
         return;
      }

      new Thread() {
         public void run() {
            final Set<WebSocketSession> sessions = registry.getAllSessions();
            for (final WebSocketSession session : sessions) {
               try {
                  session.sendMessage(new TextMessage(serializedMessage));
               } catch (Exception e) {
                  logger.warn(String.format("Failed to send message to client with" +
                        "session ID: %s", session.getId()), e);
               }
            }
         }
      }.start();
   }
}
