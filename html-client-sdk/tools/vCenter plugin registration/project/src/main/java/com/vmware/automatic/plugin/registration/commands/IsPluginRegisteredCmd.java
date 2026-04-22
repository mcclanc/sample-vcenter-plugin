/*
 * ******************************************************************
 * Copyright (c) 2016-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.automatic.plugin.registration.commands;

import com.vmware.automatic.plugin.registration.services.ConnectionService;
import com.vmware.automatic.plugin.registration.services.PluginExtensionRegistryService;
import org.apache.commons.cli.CommandLineParser;

/**
 * Implements execution of 'isPluginRegistered' command.
 */
public class IsPluginRegisteredCmd extends PluginCmd {

   public IsPluginRegisteredCmd(ConnectionService connectionService,
         PluginExtensionRegistryService registryService,
         CommandLineParser parser) {
      super(connectionService, registryService, parser);
   }
   /**
    * Check if plugin is registered
    */
   protected void doExecute() throws Exception {
      final String key = _commandLine.getOptionValue("k");
      final String url = _commandLine.getOptionValue("url");
      final String message;
      if (connectionService.getVimPort().findExtension(
            connectionService.getExtensionManager(), key) != null) {
         message = "Plugin '%s' is registered to vCenter <%s>.";
      } else {
         message = "Plugin '%s' is not registered to vCenter <%s>.";
      }
      System.out.println(String.format(message, key, url));
   }
}
