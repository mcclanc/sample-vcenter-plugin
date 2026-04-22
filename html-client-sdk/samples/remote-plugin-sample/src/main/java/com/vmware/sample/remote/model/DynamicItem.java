/*
 * ******************************************************************
 * Copyright (c) 2020-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote.model;

/**
 * Model showing the information needed by the vSphere Client in order to determine
 * whether the particular view/action should be visible in the UI
 */
public class DynamicItem {
   public String id;
   public boolean visible;

   public DynamicItem(String id, boolean visible) {
      this.id = id;
      this.visible = visible;
   }
}
