/*
 * ******************************************************************
 * Copyright (c) 2020-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote.gateway;

import com.vmware.vim25.UserSession;
import com.vmware.vim25.VimPortType;

/**
 * Contains information about the vCenter Server connection.
 */
public class VimSessionInfo {
   private VimPortType vimPort;
   private UserSession userSession;

   public VimSessionInfo(final VimPortType vimPort, final UserSession userSession) {
      this.vimPort = vimPort;
      this.userSession = userSession;
   }

   public VimPortType getVimPort() {
      return vimPort;
   }

   public void setVimPort(final VimPortType vimPort) {
      this.vimPort = vimPort;
   }

   public UserSession getUserSession() {
      return userSession;
   }

   public void setUserSession(final UserSession userSession) {
      this.userSession = userSession;
   }
}
