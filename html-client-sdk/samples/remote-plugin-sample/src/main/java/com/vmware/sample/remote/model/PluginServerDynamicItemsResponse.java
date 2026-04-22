/*
 * ******************************************************************
 * Copyright (c) 2020-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote.model;

import java.util.List;

/**
 * The model shows what response information is needed by the vSphere Client when
 * retrieving the dynamic views/actions
 */
public class PluginServerDynamicItemsResponse {

   public String apiVersion;
   public List<DynamicItem> dynamicItems;

   public PluginServerDynamicItemsResponse(String apiVersion, List<DynamicItem> dynamicItems) {
      this.apiVersion = apiVersion;
      this.dynamicItems = dynamicItems;
   }
}
