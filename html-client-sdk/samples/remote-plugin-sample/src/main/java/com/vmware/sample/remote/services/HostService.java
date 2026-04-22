/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote.services;

import java.util.List;
import com.vmware.sample.remote.model.Host;

/**
 * Interface used to retrieve information about related hosts.
 */
public interface HostService {

   /**
    * Retrieves the connected hosts
    *
    * @return connected hosts
    */
   List<Host> retrieveConnectedHosts();

   /**
    * Retrieves the connected Host object related to the given Chassis
    *
    * @return related Host objects
    */
   List<Host> retrieveConnectedHosts(String chassisId);
}
