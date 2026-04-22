/*
 * ******************************************************************
 * Copyright (c) 2016-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.automatic.plugin.registration.actions;

public enum Action {
   REGISTER_PLUGIN("registerPlugin"),
   UNREGISTER_PLUGIN("unregisterPlugin"),
   IS_PLUGIN_REGISTERED("isPluginRegistered"),
   UPDATE_PLUGIN("updatePlugin");

   private String value;

   Action(String value) {
      this.value = value;
   }

   @Override
   public String toString() {
      return value;
   }

   public String getValue() {
      return value;
   }

   public static Action fromValue(String value) {
      for (Action action : Action.values()) {
         if (action.getValue().equals(value)) {
            return action;
         }
      }
      final String message = String.format("Unsupported action '%s'", value);
      throw new IllegalArgumentException(message);
   }
}
