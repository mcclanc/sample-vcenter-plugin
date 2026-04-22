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
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * An abstract class that implements common logic for Register- and UpdateCmd.
 * Parses the common command line arguments for both commands.
 * <p>
 * <b>Command line arguments:</b>
 * -serverThumbprint <SERVER_THUMB_PRINT>               - optional, when pluginUrl is HTTPS, thumbprint of plugin server's SSL certificate
 * -name <PLUGIN_NAME>                                  - optional, name of the plugin
 * -summary <PLUGIN_SUMMARY>                            - optional, brief description
 * -company <PLUGIN_COMPANY>                            - optional, company that owns the plugin
 * -pluginServers                                       - optional, a description of additional plugin server
 */
public abstract class PluginRegistryCmd extends PluginCmd {

   public PluginRegistryCmd(ConnectionService connectionService,
         PluginExtensionRegistryService registryService,
         CommandLineParser parser) {
      super(connectionService, registryService, parser);
   }

   @Override
   public Options getPluginOpts() {
      Options options = super.getPluginOpts();
      options.addOption(
            Option.builder("n").longOpt("name").hasArg().argName("name")
                  .desc("Plugin name").required(false).build());
      options.addOption(
            Option.builder("s").longOpt("summary").hasArg().argName("summary")
                  .desc("Plugin summary").required(false).build());
      options.addOption(
            Option.builder("c").longOpt("company").hasArg().argName("company")
                  .desc("Plugin company").required(false).build());
      options.addOption(
            Option.builder("st").longOpt("serverThumbprint").hasArg()
                  .argName("plugin manifest server's thumbprint")
                  .desc("Thumbprint of the plugin manifest server's SSL certificate.")
                  .required(false).build());
      options.addOption(
            Option.builder("scf").longOpt("serverCertificateFile").hasArg()
                  .argName("plugin manifest server's certificate")
                  .desc("Plugin manifest server's SSL certificate file in PEM or DER format.")
                  .required(false).build());
      options.addOption(
            Option.builder("ps")
                  .longOpt("pluginServers")
                  .hasArg()
                  .argName("additional plugin servers")
                  .desc("A JSON formatted array of additional plugin servers." +
                        "Each object in the array must specify a 'url'. Additionally, " +
                        "each object may specify 'type', 'serverThumbprint', 'serverCertificateFile'," +
                        " 'label', 'summary', 'company' and 'adminEmail'")
                  .required(false)
                  .build());
      options.addOption(
            Option.builder(null)
                  .longOpt("taskList")
                  .hasArg()
                  .argName("path to the tasks list json file")
                  .desc("The file should contain a JSON formatted array of task infos." +
                        "Each object in the array must specify the 'taskId' of the task.")
                  .required(false)
                  .build());
      options.addOption(
            Option.builder(null)
                  .longOpt("faultList")
                  .hasArg()
                  .argName("path to the fault list json file")
                  .desc("The file should contain a JSON formatted array of fault infos." +
                        "Each object in the array must specify the 'faultId' of the fault.")
                  .required(false)
                  .build());
      options.addOption(
            Option.builder(null)
                  .longOpt("privilegeList")
                  .hasArg()
                  .argName("path to the privilege list json file")
                  .desc("The file should contain a JSON formatted array of privilege group objects." +
                        "Each object in the array must specify the 'groupId' of the privilege group " +
                        "and the 'privileges' in that group. Each object in the 'privileges' array must " +
                        "specify an 'privilegeId' of the privilege.")
                  .required(false)
                  .build());
      options.addOption(
            Option.builder(null)
                  .longOpt("eventList")
                  .hasArg()
                  .argName("path to the event list json file")
                  .desc("The file should contain a JSON formatted array of event infos." +
                        "Each object in the array must specify the 'eventId' of the event." +
                        "In addition, an optional XML descriptor for the EventType can be specified.")
                  .required(false)
                  .build());
      options.addOption(
            Option.builder(null)
                  .longOpt("resourceList")
                  .hasArg()
                  .argName("path to the resource list json file")
                  .desc("The file should contain a JSON formatted object, where" +
                        "the keys are locales (for example 'en', 'fr', 'de' ..) and the values are objects " +
                        "with key to be localized and the value is the localizable message." )
                  .required(false)
                  .build());
      return options;
   }
}
