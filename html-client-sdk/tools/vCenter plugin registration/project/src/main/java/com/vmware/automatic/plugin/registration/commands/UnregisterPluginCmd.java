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
import com.vmware.automatic.plugin.registration.services.PrintHelper;
import com.vmware.vim25.NotFoundFaultMsg;
import com.vmware.vim25.RuntimeFaultFaultMsg;

import org.apache.commons.cli.CommandLineParser;

/**
 * Implements execution of un-register plugin command.
 */
public class UnregisterPluginCmd extends PluginCmd {

   public UnregisterPluginCmd(ConnectionService connectionService,
         PluginExtensionRegistryService registryService,
         CommandLineParser parser) {
      super(connectionService, registryService, parser);
   }

   /**
    * Unregisters a single extension based on the set member fields.
    */
   protected void doExecute() throws Exception {
      final String key = _commandLine.getOptionValue("k");
      final String url = _commandLine.getOptionValue("url");
      try {
         connectionService.getVimPort()
               .unregisterExtension(connectionService.getExtensionManager(), key);
      } catch (NotFoundFaultMsg ex) {
         System.out.println(String.format(
               "Plugin with key '%s' is not registered to vCenter Server <%s>.",
               key, url));
         throw ex;
      }
      System.out.println(String.format(
            "Plugin '%s' has been successfully un-registered from vCenter Server <%s>.",
            key, url));
   }

}
