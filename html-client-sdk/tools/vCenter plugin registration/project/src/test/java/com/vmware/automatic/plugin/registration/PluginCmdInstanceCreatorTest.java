/*
 * ******************************************************************
 * Copyright (c) 2022-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.automatic.plugin.registration;

import com.vmware.automatic.plugin.registration.commands.IsPluginRegisteredCmd;
import com.vmware.automatic.plugin.registration.commands.RegisterPluginCmd;
import com.vmware.automatic.plugin.registration.commands.UnregisterPluginCmd;
import com.vmware.automatic.plugin.registration.commands.UpdatePluginCmd;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class PluginCmdInstanceCreatorTest {

   @Test
   public void getInstance_whenActionIsRegister_returnsRegisterCmd() {
      final PluginCmdInstanceCreator builder = new PluginCmdInstanceCreator();
      assertTrue(
            builder.getInstance("registerPlugin") instanceof RegisterPluginCmd);
   }

   @Test
   public void getInstance_whenActionIsUpdate_returnsUpdateCmd() {
      final PluginCmdInstanceCreator builder = new PluginCmdInstanceCreator();
      assertTrue(
            builder.getInstance("updatePlugin") instanceof UpdatePluginCmd);
   }

   @Test
   public void getInstance_whenActionIsUnregister_returnsUnregisterCmd() {
      final PluginCmdInstanceCreator builder = new PluginCmdInstanceCreator();
      assertTrue(
            builder.getInstance("unregisterPlugin") instanceof UnregisterPluginCmd);
   }

   @Test
   public void getInstance_whenActionIsIsPluginRegistered_returnsIsPluginRegisteredCmd() {
      final PluginCmdInstanceCreator builder = new PluginCmdInstanceCreator();
      assertTrue(
            builder.getInstance("isPluginRegistered") instanceof IsPluginRegisteredCmd);
   }

   @Test(expectedExceptions = IllegalArgumentException.class)
   public void getInstance_whenNoSupportedActionProvided_throwsIllegalArgumentException() {
      final PluginCmdInstanceCreator builder = new PluginCmdInstanceCreator();
      builder.getInstance("fake action");
   }

   @Test(expectedExceptions = IllegalArgumentException.class)
   public void getInstance_whenNullProvided_throwsIllegalArgumentException() {
      final PluginCmdInstanceCreator builder = new PluginCmdInstanceCreator();
      builder.getInstance(null);
   }
}
