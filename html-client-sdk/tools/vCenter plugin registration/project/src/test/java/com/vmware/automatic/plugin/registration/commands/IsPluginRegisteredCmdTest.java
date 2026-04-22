/*
 * ******************************************************************
 * Copyright (c) 2022-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.automatic.plugin.registration.commands;

import com.vmware.vim25.Extension;
import org.easymock.EasyMock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class IsPluginRegisteredCmdTest extends PluginCmdTest {

   @BeforeMethod
   public void beforeEach() throws Exception {
      super.beforeEach();
      pluginCmd = new IsPluginRegisteredCmd(connectionServiceMock,
            registryServiceMock, cmdLineParserMock);
   }

   @AfterMethod
   public void afterEach() {
      super.afterEach();
   }

   @Test
   public void execute_whenPluginExists_printsCorrectMessage()
         throws Exception {
      final Extension extensionMock = EasyMock.createMock(Extension.class);
      EasyMock
            .expect(vimPortMock.findExtension(extensionManagerMock, pluginKey))
            .andReturn(extensionMock).once();
      EasyMock.replay(vimPortMock, connectionServiceMock, cmdLineMock,
            cmdLineParserMock);
      pluginCmd.execute(cmdLineArgsMock);
      EasyMock.verify(vimPortMock, connectionServiceMock, cmdLineMock,
            cmdLineParserMock);
      final String expectedMsg = String
            .format("Plugin '%s' is registered to vCenter <%s>.%s", pluginKey,
                  vcUrl, System.lineSeparator());
      assertEquals(printedContent.toString(), expectedMsg);
   }

   @Test
   public void execute_whenPluginDoesNotExist_printsCorrectMessage()
         throws Exception {
      EasyMock
            .expect(vimPortMock.findExtension(extensionManagerMock, pluginKey))
            .andReturn(null).once();
      EasyMock.replay(vimPortMock, connectionServiceMock, cmdLineMock,
            cmdLineParserMock);
      pluginCmd.execute(cmdLineArgsMock);
      EasyMock.verify(vimPortMock, connectionServiceMock, cmdLineMock,
            cmdLineParserMock);
      final String expectedMsg = String
            .format("Plugin '%s' is not registered to vCenter <%s>.%s",
                  pluginKey, vcUrl, System.lineSeparator());
      assertEquals(printedContent.toString(), expectedMsg);
   }
}
