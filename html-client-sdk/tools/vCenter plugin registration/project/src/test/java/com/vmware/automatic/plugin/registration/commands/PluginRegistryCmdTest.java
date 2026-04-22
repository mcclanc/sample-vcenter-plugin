/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.automatic.plugin.registration.commands;

import org.apache.commons.cli.Options;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public class PluginRegistryCmdTest extends PluginCmdTest {

   @BeforeMethod
   void beforeEach() throws Exception {
      super.beforeEach();
   }

   @AfterMethod
   public void afterEach() {
      super.afterEach();
   }


   @Override
   void assertOptions(Options options) {
      super.assertOptions(options);
      // name
      assertOption(options, "n", "name", "Plugin name", false, true, "name");
      // summary
      assertOption(options, "s", "summary", "Plugin summary", false, true,
            "summary");
      // company
      assertOption(options, "c", "company", "Plugin company", false, true,
            "company");
      // server thumbprint
      assertOption(options, "st", "serverThumbprint",
            "Thumbprint of the plugin manifest server's SSL certificate.",
            false, true, "plugin manifest server's thumbprint");
   }
}
