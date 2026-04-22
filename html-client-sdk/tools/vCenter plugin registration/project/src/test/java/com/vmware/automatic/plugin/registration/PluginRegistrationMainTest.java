/*
 * ******************************************************************
 * Copyright (c) 2022-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.automatic.plugin.registration;

import org.apache.commons.cli.ParseException;
import org.testng.annotations.Test;

public class PluginRegistrationMainTest {

   @Test(expectedExceptions = ParseException.class)
   public void main_whenNoActionProvided_printsManual() throws Exception {
      PluginRegistrationMain.main(new String[]{});
   }

//   @Test
//   public void main_whenCorrectActionProvided_
}
