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
 * Data model of a chassis object.
 */
public class Chassis {

   public String id;
   public String name;
   public String dimensions;
   public String serverType;
   public boolean isActive;
   public List<String> relatedHostsIds;

   public Chassis() {
      // A default constructor is needed for the JSON serialization to work.
   }
}
