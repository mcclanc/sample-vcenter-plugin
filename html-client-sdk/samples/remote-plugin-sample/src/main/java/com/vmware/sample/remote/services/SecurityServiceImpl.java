/*
 * ******************************************************************
 * Copyright (c) 2020-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

package com.vmware.sample.remote.services;

import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vmware.sample.remote.gateway.GatewayCredentials;
import com.vmware.sample.remote.gateway.SessionService;
import com.vmware.sample.remote.gateway.VimSessionInfo;

/**
 * Implementation of SecurityService.
 */
public class SecurityServiceImpl implements SecurityService {

   private static final Log logger = LogFactory.getLog(SecurityServiceImpl.class);

   private final SessionService _sessionService;

   public SecurityServiceImpl(final SessionService sessionService) {
      _sessionService = sessionService;
   }

   @Override
   public boolean validateGatewayCredentials(
         final GatewayCredentials credentials) {
      Validate.notNull(credentials);

      VimSessionInfo vimSessionInfo = null;
      Exception exception = null;
      try {
         vimSessionInfo = _sessionService.getVimSessionInfo(credentials);
      } catch (Exception e) {
         exception = e;
      }

      if (exception != null) {
         logger.error("Gateway credentials validation failed" +
               " due to an error", exception);
         return false;
      }

      if (vimSessionInfo == null) {
         logger.error("Gateway credentials validation failed" +
               " due to null VimSessionInfo.");
         return false;
      }

      return true;
   }
}
