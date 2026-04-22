/*
 * ******************************************************************
 * Copyright (c) 2020-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote.gateway;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.Validate;

/**
 * Contains session ID information obtained from a HttpServletRequest.
 */
public class GatewayCredentials {
   public static final String API_SESSION_ID_NAME = "vmware-api-session-id";

   public final String sessionId;

   private GatewayCredentials(final String sessionId) {
      Validate.notNull(sessionId);

      this.sessionId = sessionId;
   }

   public static GatewayCredentials fromRequestHeaders(HttpServletRequest request) {
      final String sessionId = request.getHeader(API_SESSION_ID_NAME);

      return new GatewayCredentials(sessionId);
   }

   public static GatewayCredentials fromRequestParameters(HttpServletRequest request) {
      final String sessionId = request.getParameter(API_SESSION_ID_NAME);

      return new GatewayCredentials(sessionId);
   }
}
