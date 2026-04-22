/*
 * ******************************************************************
 * Copyright (c) 2016-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.automatic.plugin.registration;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;

/**
 * Main class for invoking plugin registration commands.
 */
public class PluginRegistrationMain {

   public static void main(String[] args) throws Exception {
      final CommandLineParser parser = new DefaultParser();
      final PluginCmdInstanceCreator builder = new PluginCmdInstanceCreator();
      new PluginRegistrationEntryPoint(args, builder, parser).execute();
   }
}
