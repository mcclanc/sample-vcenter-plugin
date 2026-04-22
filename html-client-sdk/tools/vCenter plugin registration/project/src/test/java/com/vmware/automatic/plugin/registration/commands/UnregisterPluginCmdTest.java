/*
 * ******************************************************************
 * Copyright (c) 2022-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.automatic.plugin.registration.commands;

import org.easymock.EasyMock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class UnregisterPluginCmdTest extends PluginCmdTest {

   @BeforeMethod
   public void beforeEach() throws Exception {
      super.beforeEach();
      pluginCmd = new UnregisterPluginCmd(connectionServiceMock,
            registryServiceMock, cmdLineParserMock);
   }

   @AfterMethod
   public void afterEach() {
      super.afterEach();
   }

   @Test
   public void execute_perfromsAsExpected() throws Exception {
      // Expect vimPort.unregister() call
      vimPortMock.unregisterExtension(extensionManagerMock, pluginKey);
      EasyMock.expectLastCall();
      EasyMock.replay(vimPortMock, connectionServiceMock, cmdLineMock,
            cmdLineParserMock);
      pluginCmd.execute(cmdLineArgsMock);
      EasyMock.verify(vimPortMock, connectionServiceMock, cmdLineMock,
            cmdLineParserMock);

      // Verify message
      final String expectedMsg = String.format(
            "Plugin '%s' has been successfully un-registered from vCenter Server <%s>.%s",
            pluginKey, vcUrl, System.lineSeparator());
      assertEquals(printedContent.toString(), expectedMsg);
   }
}
