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
import com.vmware.vim25.Extension;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Parses the provided command line arguments for register plugin command.
 * Implements register plugin command execution.
 * <p>
 * <b>Specific command line arguments for register plugin command</b>
 * -pluginUrl <https://DOWNLOAD_LOCATION/MY_PLUGIN.JSON> - <b>required</b>, location of plugin .json file
 * -version <PLUGIN_VERSION>                            - <b>required</b>, plugin version
 *
 */
public class RegisterPluginCmd extends PluginRegistryCmd {

   public RegisterPluginCmd(ConnectionService connectionService,
         PluginExtensionRegistryService registryService,
         CommandLineParser parser) {
      super(connectionService, registryService, parser);
   }

   @Override
   public Options getPluginOpts() {
      Options options = super.getPluginOpts();
      options.addOption(Option.builder("pu").longOpt("pluginUrl").hasArg()
            .argName("plugin manifest url").required()
            .desc("Url where the plugin manifest will be downloaded from.").build());
      options.addOption(
            Option.builder("v").longOpt("version").hasArg().argName("version")
                  .required().desc("Plugin version.").build());
      return options;
   }

   /**
    * Registers a single extension based on the set member fields.
    */
   protected void doExecute() throws Exception {
      final String url = _commandLine.getOptionValue("url");
      final String key = _commandLine.getOptionValue("k");
      final Extension extension = new Extension();

      registryService.updateTopLevelProperties(extension, _commandLine);
      registryService.updateDescription(extension, _commandLine);
      registryService.updateClientInfo(extension, _commandLine);
      registryService.updateTaskList(extension, _commandLine);
      registryService.updateFaultList(extension, _commandLine);
      registryService.updatePrivilegeList(extension, _commandLine);
      registryService.updateEventList(extension, _commandLine);
      registryService.updateResourceInfo(extension, _commandLine);
      registryService.updateServerInfo(extension, _commandLine);

      registryService.updatelastHeartbeatTime(extension);
      connectionService.getVimPort()
            .registerExtension(connectionService.getExtensionManager(),
                  extension);
      System.out.println(String.format(
            "Plugin '%s' has been successfully registered to vCenter <%s>.",
            key, url));
   }
}
