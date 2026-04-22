/*
 * ******************************************************************
 * Copyright (c) 2022-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.automatic.plugin.registration.resources;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * A helper test class used to simulate user provided command line arguments
 */
public class CommandLineBuilder {

   private final Options options = new Options();
   private final List<String> cmdLineArgs = new ArrayList();

   public CommandLineBuilder key(String key) {
      options.addOption(
            Option.builder("k").longOpt("key").hasArg().argName("key")
                  .desc("Plugin key").required().build());
      cmdLineArgs.add("-k");
      cmdLineArgs.add(key);
      return this;
   }

   public CommandLineBuilder url(String url) {
      options.addOption(Option.builder("url").longOpt("vcenterUrl").hasArg()
            .argName("vc url").desc("vCenter server URL").required().build());
      cmdLineArgs.add("-url");
      cmdLineArgs.add(url);
      return this;
   }

   public CommandLineBuilder username(String username) {
      options.addOption(
            Option.builder("u").longOpt("username").hasArg().argName("vc user")
                  .desc("vCenter server username").required().build());
      cmdLineArgs.add("-u");
      cmdLineArgs.add(username);
      return this;
   }

   public CommandLineBuilder password(String password) {
      options.addOption(
            Option.builder("p").longOpt("password").hasArg().argName("vc pass")
                  .desc("vCenter server password. Note that passing your password "
                        + "as command line argument may be insecure. "
                        + "Skipping this argument will make the program require "
                        + "the password over stdin while connecting to the endpoint.")
                  .build());
      cmdLineArgs.add("-p");
      cmdLineArgs.add(password);
      return this;
   }

   public CommandLineBuilder version(String version) {
      options.addOption(
            Option.builder("v").longOpt("version").hasArg().argName("version")
                  .desc("Plugin version").build());
      cmdLineArgs.add("-v");
      cmdLineArgs.add(version);
      return this;
   }

   public CommandLineBuilder pluginUrl(String pluginUrl) {
      options.addOption(Option.builder("pu").longOpt("pluginUrl").hasArg()
            .argName("plugin url")
            .desc("Url from where the plugin will be downloaded").build());
      cmdLineArgs.add("-pu");
      cmdLineArgs.add(pluginUrl);
      return this;
   }

   public CommandLineBuilder label(String label) {
      options.addOption(
            Option.builder("n").longOpt("name").hasArg().argName("name")
                  .desc("Plugin name").required(false).build());
      cmdLineArgs.add("-n");
      cmdLineArgs.add(label);
      return this;
   }

   public CommandLineBuilder summary(String summary) {
      options.addOption(
            Option.builder("s").longOpt("summary").hasArg().argName("summary")
                  .desc("Plugin summary").required(false).build());
      cmdLineArgs.add("-s");
      cmdLineArgs.add(summary);
      return this;
   }

   public CommandLineBuilder company(String company) {
      options.addOption(
            Option.builder("c").longOpt("company").hasArg().argName("company")
                  .desc("Plugin company").required(false).build());
      cmdLineArgs.add("-c");
      cmdLineArgs.add(company);
      return this;
   }

   public CommandLineBuilder serverThumbprint(String serverThumbprint) {
      options.addOption(
            Option.builder("st").longOpt("serverThumbprint").hasArg()
                  .argName("plugin manifest server's thumbprint")
                  .desc("Thumbprint of the server from which the plugin will be downloaded.")
                  .required(false).build());
      cmdLineArgs.add("-st");
      cmdLineArgs.add(serverThumbprint);
      return this;
   }

   public CommandLineBuilder serverCertificateFile(final String serverCertificateFile) {
      options.addOption(
            Option.builder("scf").longOpt("serverCertificateFile").hasArg()
                  .argName("plugin manifest server's certificate")
                  .desc("Plugin manifest server's SSL certificate file in PEM or DER format.")
                  .required(false).build());
      cmdLineArgs.add("-scf");
      cmdLineArgs.add(serverCertificateFile);
      return this;
   }

   public CommandLineBuilder vcServerThumbprint(String vcServerThumbprint) {
      options.addOption(
            Option.builder("vct").longOpt("vcenterServerThumbprint").hasArg()
                  .argName("vCenter server thumbprint")
                  .desc("vCenter server thumbprint")
                  .required(false).build());
      cmdLineArgs.add("-vct");
      cmdLineArgs.add(vcServerThumbprint);
      return this;
   }

   public CommandLineBuilder pluginServers(String pluginServersJson) {
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
      cmdLineArgs.add("-ps");
      cmdLineArgs.add(pluginServersJson);
      return this;
   }

   public CommandLineBuilder taskList(String taskListFilePath) {
      options.addOption(
            Option.builder(null)
                  .longOpt("taskList")
                  .hasArg()
                  .argName("path to the tasks list file")
                  .desc("The file should contain a JSON formatted array of task infos." +
                        "Each object in the array must specify the 'taskId' of the task.")
                  .required(false)
                  .build()
      );

      cmdLineArgs.add("--taskList");
      cmdLineArgs.add(taskListFilePath);
      return this;
   }

   public CommandLineBuilder faultList(String faultListFilePath) {
      options.addOption(
            Option.builder(null)
                  .longOpt("faultList")
                  .hasArg()
                  .argName("path to the fault list file")
                  .desc("The file should contain a JSON formatted array of fault infos." +
                        "Each object in the array must specify the 'faultId' of the fault.")
                  .required(false)
                  .build()
      );

      cmdLineArgs.add("--faultList");
      cmdLineArgs.add(faultListFilePath);
      return this;
   }

   public CommandLineBuilder privilegeList(String privilegeListFilePath) {
      options.addOption(
            Option.builder(null)
                  .longOpt("privilegeList")
                  .hasArg()
                  .argName("path to the privilege list file")
                  .desc("The file should contain a JSON formatted array of privilege group objects." +
                        "Each object in the array must specify the 'groupId' of the privilege group " +
                        "and the 'privileges' in that group. Each object in the 'privileges' array must " +
                        "specify an 'privilegeId' of the privilege.")
                  .required(false)
                  .build()
      );

      cmdLineArgs.add("--privilegeList");
      cmdLineArgs.add(privilegeListFilePath);
      return this;
   }

   public CommandLineBuilder eventList(String eventListFilePath) {
      options.addOption(
            Option.builder(null)
                  .longOpt("eventList")
                  .hasArg()
                  .argName("path to the event list json file")
                  .desc("The file should contain a JSON formatted array of event infos." +
                        "Each object in the array must specify the 'eventId' of the event." +
                        "In addition, an optional XML descriptor for the EventType can be specified.")
                  .required(false)
                  .build()
      );

      cmdLineArgs.add("--eventList");
      cmdLineArgs.add(eventListFilePath);
      return this;
   }

   public CommandLineBuilder resourceList(String resourceListFilePath) {
      options.addOption(
            Option.builder(null)
                  .longOpt("resourceList")
                  .hasArg()
                  .argName("path to the resource list file")
                  .desc("The file should contain a JSON formatted object, where" +
                        "the keys are locales (for example 'en', 'fr', 'de' ..) and the values are objects " +
                        "with key to be localized and the value is the localizable message." )
                  .required(false)
                  .build()
      );

      cmdLineArgs.add("--resourceList");
      cmdLineArgs.add(resourceListFilePath);
      return this;
   }

   public CommandLineBuilder defaults() {
      return this.key("plugin-key").url("https://my-vcenter-server.com/sdk")
            .username("administrator@vsphere.local")
            .password("TEST_MOCK_VCENTER_PASSWORD");
   }

   public CommandLine build() throws ParseException {
      return new DefaultParser()
            .parse(options, cmdLineArgs.toArray(new String[0]));
   }
}
