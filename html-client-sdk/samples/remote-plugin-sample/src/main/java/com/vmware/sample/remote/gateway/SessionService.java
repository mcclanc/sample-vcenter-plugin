/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote.gateway;

public interface SessionService {

   VimSessionInfo getVimSessionInfo();

   VimSessionInfo getVimSessionInfo(final GatewayCredentials credentials);
}
