/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.automatic.plugin.registration.commands;

import com.vmware.automatic.plugin.registration.services.ConnectionService;
import com.vmware.automatic.plugin.registration.services.PluginExtensionRegistryService;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VimPortType;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.easymock.EasyMock;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class PluginCmdTest {
   ConnectionService connectionServiceMock;
   PluginExtensionRegistryService registryServiceMock;
   CommandLineParser cmdLineParserMock;
   PluginCmd pluginCmd;
   CommandLine cmdLineMock;
   String[] cmdLineArgsMock;
   VimPortType vimPortMock;
   ManagedObjectReference extensionManagerMock;
   String pluginKey;
   String vcUrl;
   ByteArrayOutputStream printedContent;
   PrintStream originalSystemOut;

   void beforeEach() throws Exception {
      pluginKey = "test-key";
      vcUrl = "test-url";
      connectionServiceMock = EasyMock.createMock(ConnectionService.class);
      registryServiceMock = EasyMock
            .createMock(PluginExtensionRegistryService.class);
      cmdLineParserMock = EasyMock.createMock(DefaultParser.class);
      cmdLineArgsMock = new String[] {};
      cmdLineMock = EasyMock.createMock(CommandLine.class);
      vimPortMock = EasyMock.createMock(VimPortType.class);
      extensionManagerMock = EasyMock.createMock(ManagedObjectReference.class);

      // expect parse
      EasyMock.expect(cmdLineParserMock.parse(EasyMock.anyObject(Options.class),
            EasyMock.eq(cmdLineArgsMock))).andReturn(cmdLineMock).once();
      // expect cmdLine calls
      EasyMock.expect(cmdLineMock.getOptionValue(EasyMock.anyString()))
            .andReturn(pluginKey).once().andReturn(vcUrl);
      // expect connect
      connectionServiceMock.connect(cmdLineMock);
      EasyMock.expectLastCall().once();

      // expect get vimPort and extensionManager instances
      EasyMock.expect(connectionServiceMock.getVimPort()).andReturn(vimPortMock)
            .once();
      EasyMock.expect(connectionServiceMock.getExtensionManager())
            .andReturn(extensionManagerMock).once();

      // expect disconnect
      connectionServiceMock.disconnect(cmdLineMock);
      EasyMock.expectLastCall().once();

      originalSystemOut = System.out;

      // Mock System.out default 'out'
      printedContent = new ByteArrayOutputStream();
      System.setOut(new PrintStream(printedContent));
   }

   void afterEach() {
      System.setOut(originalSystemOut);
   }

   void assertOptions(Options options) {
      // action
      assertOption(options, "action", null,
            "registerPlugin, unregisterPlugin, updatePlugin, isPluginRegistered",
            true, true, "action");
      // url
      assertOption(options, "url", null, "vCenter server SDK URL. The url format is https://vc-ip/sdk", true, true,
            "vc url");
      // password
      assertOption(options, "p", "password", "vCenter server password. "
            + "Note that passing your password as command line argument may be insecure. "
            + "Skipping this argument will make the program require the password over stdin "
            + "while connecting to the endpoint.", false, true, "vc pass");
      // username
      assertOption(options, "u", "username", "vCenter server username", true,
            true, "vc user");
      // key
      assertOption(options, "k", "key", "Plugin key", true, true, "key");

      // vcenterServer thumbprint
      assertOption(options, "vct", "vcenterServerThumbprint", "vCenter server thumbprint", false, true, "vCenter server thumbprint");

      // insecure parameter
      assertOption(options, "insecure", null, "Connect to vCenter Server insecurely by trusting "
            + "both certificate and hostname. VMware does not recommend using "
            + "this option unless during development.", false, false, null);
   }

   void assertOption(Options options, String shortOption, String longOption,
         String description, boolean required) {
      assertOption(options, shortOption, longOption, description, required,
            false, null);
   }

   void assertOption(Options options, String shortOption, String longOption,
         String description, boolean required, boolean hasArg, String argName) {
      assertTrue(options.hasOption(shortOption));
      assertEquals(options.hasLongOption(longOption), longOption != null);
      assertEquals(options.getOption(shortOption).isRequired(), required);
      assertEquals(options.getOption(shortOption).hasArg(), hasArg);
      assertEquals(options.getOption(shortOption).getDescription(),
            description);
      if (hasArg) {
         assertEquals(options.getOption(shortOption).getArgName(), argName);
      } else {
         assertNull(options.getOption(shortOption).getArgName());
      }
   }
}
