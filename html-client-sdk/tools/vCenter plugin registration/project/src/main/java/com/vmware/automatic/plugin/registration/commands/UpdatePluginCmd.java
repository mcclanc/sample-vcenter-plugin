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
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VimPortType;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Parses the provided command line arguments for update plugin command.
 * Implements update plugin command execution.
 * <p>
 * <b>Specific command line arguments for update plugin command</b>
 * -pluginUrl <https://DOWNLOAD_LOCATION/MY_PLUGIN.json> - <b>optional</b>, location of plugin .json file.
 * -version <PLUGIN_VERSION>                            - <b>optional</b>, plugin version.
 */
public class UpdatePluginCmd extends PluginRegistryCmd {

   public UpdatePluginCmd(ConnectionService connectionService,
         PluginExtensionRegistryService registryService,
         CommandLineParser parser) {
      super(connectionService, registryService, parser);
   }

   @Override
   public Options getPluginOpts() {
      Options options = super.getPluginOpts();
      options.addOption(Option.builder("pu").longOpt("pluginUrl").hasArg()
            .argName("plugin manifest url").required(false)
            .desc("Url where the plugin manifest will be downloaded from.").build());
      options.addOption(
            Option.builder("v").longOpt("version").hasArg().argName("version")
                  .required(false).desc("Plugin version.").build());
      return options;
   }

   /**
    * Updates a single extension based on the command line arguments.
    */
   protected void doExecute() throws Exception {
      final String key = _commandLine.getOptionValue("k");
      final String url = _commandLine.getOptionValue("url");
      final VimPortType vimPort = connectionService.getVimPort();
      final ManagedObjectReference extensionManager = connectionService
            .getExtensionManager();

      final Extension extension = vimPort.findExtension(extensionManager, key);
      if (extension == null) {
         throw new RuntimeException(
               String.format("Plugin '%s' is not registered with vCenter <%s>.",
                     key, url));
      }

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

      vimPort.updateExtension(extensionManager, extension);
      System.out.println(String.format(
            "Plugin '%s', registered to vCenter Server <%s>, has been successfully updated.", key,
            url));
   }
}
