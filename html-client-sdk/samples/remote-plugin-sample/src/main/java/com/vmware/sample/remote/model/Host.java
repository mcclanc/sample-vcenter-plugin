/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote.model;

import java.util.List;

/**
 * Data model of a host object.
 */
public class Host {

   public String id;
   public String name;
   public String state;
   public String vCenterName;
   public String memorySize;
   public String numCpus;
   public List<String> relatedChassisIds;

   public Host(String id, String name, String state, String vCenterName, String memorySize, String numCpus) {
      this.id = id;
      this.name = name;
      this.state = state;
      this.vCenterName = vCenterName;
      this.memorySize = memorySize;
      this.numCpus = numCpus;
   }

   public Host() {
      // A default constructor is needed for the JSON serialization to work.
   }
}
