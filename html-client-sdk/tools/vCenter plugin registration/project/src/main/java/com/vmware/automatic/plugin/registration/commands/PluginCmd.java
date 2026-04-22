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
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import jakarta.xml.ws.soap.SOAPFaultException;

/**
 * Base abstract class that:
 * - parses the common command line arguments for all supported commands
 * - sets the workflow of the supported commands' execution
 *
 * Required arguments:
 * -url <https://IP_OF_THE_VCENTER/sdk/> - <b>required</b> argument, vCenter server IP
 * -username <USERNAME>                  - <b>required</b> argument, vCenter server username
 * -key <MY_PLUGIN_KEY>                  - <b>required</b> argument, plugin key
 * -password <PASSWORD>                  - <b>optional</b> argument, vCenter server password
 * -vct <vCenter Server Thumbprint>      - <b>optional</b> argument, vCenter server certificate thumbprint
 * -insecure <INSECURE CONNECTION>       - <b>optional</b> argument, trust all certificates when connecting to vCenter Server
 */
public abstract class PluginCmd {
   final ConnectionService connectionService;
   final PluginExtensionRegistryService registryService;
   final CommandLineParser parser;
   CommandLine _commandLine;

   public PluginCmd(ConnectionService connectionService,
         PluginExtensionRegistryService registryService,
         CommandLineParser parser) {
      this.registryService = registryService;
      this.connectionService = connectionService;
      this.parser = parser;
   }

   /**
    * Name and description of command line arguments
    */
   public Options getPluginOpts() {
      final Options options = new Options();
      options.addOption(Option.builder("action").hasArg().argName("action")
            .desc("registerPlugin, unregisterPlugin, updatePlugin, isPluginRegistered")
            .required().build());
      options.addOption(Option.builder("k").longOpt("key").hasArg().argName("key").desc("Plugin key").required()
            .build());
      options.addOption(Option.builder("url").hasArg().argName("vc url")
            .desc("vCenter server SDK URL. The url format is https://vc-ip/sdk").required().build());
      options.addOption(Option.builder("u").longOpt("username").hasArg().argName("vc user")
            .desc("vCenter server username").required().build());
      options.addOption(Option.builder("p").longOpt("password").hasArg().argName("vc pass")
            .desc("vCenter server password. Note that passing your password as command line argument may be insecure. "
                  + "Skipping this argument will make the program require the password over stdin while connecting to the endpoint.").build());
      options.addOption(Option.builder("vct").longOpt("vcenterServerThumbprint").hasArg().argName("vCenter server thumbprint")
            .desc("vCenter server thumbprint").build());
      options.addOption(Option.builder("insecure").desc("Connect to "
            + "vCenter Server insecurely by trusting both certificate and hostname. "
            + "VMware does not recommend using this option unless during development.")
            .build());
      return options;
   }

   /**
    * Connects to the vCenter, executes the current command and disconnects.
    *
    * @param args - command line arguments
    * @throws Exception
    */
   public void execute(String[] args) throws Exception {
      try {
         parseCommandLineArguments(args);
         connect();
         doExecute();
      } catch (ParseException e) {
         PrintHelper.printHelp(getPluginOpts(), e.getMessage());
         throw new ParseException(e.getMessage());
      } catch (Exception e) {
         String msg = String.format(
               "[ERROR] An error occurred while executing the following action: '%s'",
               _commandLine.getOptionValue("action"));
         throw new RuntimeException(msg, e);
      } finally {
         try {
            disconnect();
         } catch (SOAPFaultException e) {
            PrintHelper.printSoapFaultException(e);
            throw e;
         } catch (Exception e) {
            System.out.println("Failed to disconnect - " + e.getMessage());
            throw new RuntimeException(e);
         }
      }
   }

   /**
    * Parses the command line input parameters.
    * @param commandLineArgs - the program's input parameters
    * @throws ParseException
    */
   private void parseCommandLineArguments(String[] commandLineArgs)
         throws ParseException {
      final Options options = getPluginOpts();
      _commandLine = parser.parse(options, commandLineArgs);
   }

   /**
    * Establishes user session with the vCenter server.
    */
   private void connect() {
      connectionService.connect(_commandLine);
   }

   /**
    * Disconnects the user session.
    */
   private void disconnect() {
      connectionService.disconnect(_commandLine);
   }

   /**
    * Implements the workflow of the command execution.
    */
   protected abstract void doExecute() throws Exception;

}
