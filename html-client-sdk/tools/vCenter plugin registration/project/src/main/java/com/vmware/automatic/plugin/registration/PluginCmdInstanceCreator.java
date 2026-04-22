/*
 * ******************************************************************
 * Copyright (c) 2016-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.automatic.plugin.registration;

import com.vmware.automatic.plugin.registration.actions.Action;
import com.vmware.automatic.plugin.registration.commands.IsPluginRegisteredCmd;
import com.vmware.automatic.plugin.registration.commands.PluginCmd;
import com.vmware.automatic.plugin.registration.commands.RegisterPluginCmd;
import com.vmware.automatic.plugin.registration.commands.UnregisterPluginCmd;
import com.vmware.automatic.plugin.registration.commands.UpdatePluginCmd;
import com.vmware.automatic.plugin.registration.services.ConnectionService;
import com.vmware.automatic.plugin.registration.services.PluginExtensionRegistryService;
import com.vmware.automatic.plugin.registration.services.SslTrustStrategy;
import com.vmware.vim25.VimService;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;

/**
 * A helper class that creates {@link PluginCmd} instance.
 */
public class PluginCmdInstanceCreator {

   /**
    * Creates and returns the correct PluginCmd instance.
    *
    * @param actionValue - the command line provided argument name
    * @return the correct {@link PluginCmd} instance.
    */
   public PluginCmd getInstance(final String actionValue) {
      final Action action = Action.fromValue(actionValue);
      final ConnectionService connectionService = new ConnectionService(
            new VimService(), new SslTrustStrategy());
      final PluginExtensionRegistryService registryService = new PluginExtensionRegistryService();
      final CommandLineParser cmdLineParser = new DefaultParser();
      switch (action) {
      case REGISTER_PLUGIN:
         return new RegisterPluginCmd(connectionService, registryService,
               cmdLineParser);
      case UPDATE_PLUGIN:
         return new UpdatePluginCmd(connectionService, registryService,
               cmdLineParser);
      case UNREGISTER_PLUGIN:
         return new UnregisterPluginCmd(connectionService, registryService,
               cmdLineParser);
      case IS_PLUGIN_REGISTERED:
         return new IsPluginRegisteredCmd(connectionService, registryService,
               cmdLineParser);
      default:
         return null;
      }
   }
}
