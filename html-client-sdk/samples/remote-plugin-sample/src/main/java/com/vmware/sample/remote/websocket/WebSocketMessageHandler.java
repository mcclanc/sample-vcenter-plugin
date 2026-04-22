/*
 * ******************************************************************
 * Copyright (c) 2020-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

package com.vmware.sample.remote.websocket;

import java.io.IOException;

import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

public class WebSocketMessageHandler extends AbstractWebSocketHandler {

   private static final Log logger = LogFactory.getLog(WebSocketMessageHandler.class);

   private final WebSocketSessionRegistry _registry;

   public WebSocketMessageHandler(final WebSocketSessionRegistry registry) {
      Validate.notNull(registry);

      _registry = registry;
   }

   @Override
   public void afterConnectionEstablished(final WebSocketSession session) {
      _registry.addSession(session);

   }

   @Override
   public void afterConnectionClosed(final WebSocketSession session,
         final CloseStatus status) {
      _registry.removeSession(session);
   }

   @Override
   protected void handleTextMessage(final WebSocketSession session,
         final TextMessage message) {
      try {
         session.close(CloseStatus.NOT_ACCEPTABLE.withReason(
               "Text messages not supported"));
      } catch (IOException e) {
         logger.warn("Failed to close session", e);
      }
   }

   @Override
   protected void handleBinaryMessage(final WebSocketSession session,
         final BinaryMessage message) {
      try {
         session.close(CloseStatus.NOT_ACCEPTABLE.withReason(
               "Binary messages not supported"));
      } catch (IOException e) {
         logger.warn("Failed to close session", e);
      }
   }

   @Override
   public void handleTransportError(final WebSocketSession session,
         final Throwable exception) throws Exception {
      logger.error("Transport error", exception);
      session.close(CloseStatus.SERVER_ERROR);
   }
}
