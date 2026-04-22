/*
 * ******************************************************************
 * Copyright (c) 2020-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

package com.vmware.sample.remote.services;

import com.vmware.sample.remote.gateway.GatewayCredentials;

/**
 * Interface used for security related operations.
 */
public interface SecurityService {

   /**
    * Validates gateway credentials.
    *
    * @param credentials to validate
    *
    * @return whether the credentials are valid.
    */
   boolean validateGatewayCredentials(final GatewayCredentials credentials);
}
