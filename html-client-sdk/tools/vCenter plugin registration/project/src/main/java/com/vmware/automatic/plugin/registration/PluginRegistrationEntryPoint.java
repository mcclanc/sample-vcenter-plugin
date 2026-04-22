/*
 * ******************************************************************
 * Copyright (c) 2016-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.automatic.plugin.registration;

import com.vmware.automatic.plugin.registration.actions.Action;
import com.vmware.automatic.plugin.registration.commands.PluginCmd;
import com.vmware.automatic.plugin.registration.services.PrintHelper;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * The entry point of the plugin-registration-tool
 * Based on the provided input parameters instantiates the correct
 * PluginCmd class and performs its execute()
 */
public class PluginRegistrationEntryPoint {

   private final PluginCmdInstanceCreator pluginCmdInstanceCreator;
   private final CommandLineParser parser;
   private final String[] cmdLineArguments;
   private static final String ACTION_ARG = "action";
   private static final String ARGS_DELIMITER = "-";

   public PluginRegistrationEntryPoint(String[] cmdLineArguments,
         PluginCmdInstanceCreator pluginCmdInstanceCreator, CommandLineParser parser) {
      this.cmdLineArguments = cmdLineArguments;
      this.pluginCmdInstanceCreator = pluginCmdInstanceCreator;
      this.parser = parser;
   }

   /**
    * Parses -action argument, creates the command instance based on
    * the -action argument and executes the command.
    */
   public void execute() throws Exception {
      final Options options = new Options();
      options.addOption(Option.builder(ACTION_ARG).hasArg().required()
            .desc("Action to perform.").hasArg().argName(
                  String.format("%s, %s, %s, %s",
                  Action.REGISTER_PLUGIN,
                  Action.UNREGISTER_PLUGIN,
                  Action.UPDATE_PLUGIN,
                  Action.IS_PLUGIN_REGISTERED)).build());
      final CommandLine commandLine;
      try {
         commandLine = parser
               .parse(options, extractActionArg(cmdLineArguments), true);
      } catch (ParseException e) {
         PrintHelper.printHelp(options, e.getMessage());
         throw new ParseException(e.getMessage());
      }
      final String action = commandLine.getOptionValue(ACTION_ARG);
      final PluginCmd pluginCmd = pluginCmdInstanceCreator.getInstance(action);

      pluginCmd.execute(cmdLineArguments);
   }

   /**
    * Extracts the -action argument and its value from the
    * command line arguments array to determine which command to perform.
    *
    * @param cmdLineArguments - command line arguments
    * @return the -action argument and its value, if they exist,
    * or the cmdLineArguments input parameter otherwise
    */
   private String[] extractActionArg(final String[] cmdLineArguments) {
      if (cmdLineArguments != null) {
         for (int i = 0; i < cmdLineArguments.length; i++) {
            if (ARGS_DELIMITER.concat(ACTION_ARG).equals(cmdLineArguments[i])) {
               // If the next element exists (hence there is a value for -action)
               if (cmdLineArguments.length > i + 1 && !cmdLineArguments[i + 1]
                     .startsWith(ARGS_DELIMITER)) {
                  return new String[] { cmdLineArguments[i],
                        cmdLineArguments[i + 1] };
               }
               return new String[] { cmdLineArguments[i] };
            }
         }
      }
      return cmdLineArguments;
   }
}
