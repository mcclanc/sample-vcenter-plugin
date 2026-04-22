/*
 * ******************************************************************
 * Copyright (c) 2020-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.automatic.plugin.registration.commands;

import com.vmware.vim25.Extension;
import com.vmware.vim25.ManagedObjectReference;
import org.apache.commons.cli.Options;
import org.easymock.EasyMock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UpdatePluginCmdTest extends PluginRegistryCmdTest {

   @BeforeMethod
   public void beforeEach() throws Exception {
      super.beforeEach();
      pluginCmd = new UpdatePluginCmd(connectionServiceMock,
            registryServiceMock, cmdLineParserMock);
   }

   @AfterMethod
   public void afterEach() {
      super.afterEach();
   }

   @Test
   void getPluginOptions_setsCorrectOptions() {
      Options options = pluginCmd.getPluginOpts();
      super.assertOptions(options);
      // key
      assertOption(options, "pu", "pluginUrl",
            "Url where the plugin manifest will be downloaded from.", false, true,
            "plugin manifest url");
      // version
      assertOption(options, "v", "version", "Plugin version.", false, true,
            "version");
   }

   @Test
   public void execute_whenExtensionExists_updatesTheExtension()
         throws Exception {
      Extension extensionMock = EasyMock.createMock(Extension.class);
      // Expect the extension found
      EasyMock.expect(vimPortMock
            .findExtension(EasyMock.anyObject(ManagedObjectReference.class),
                  EasyMock.eq(pluginKey))).andReturn(extensionMock).once();
      // expect registry service calls - this is the actual command behaviour
      registryServiceMock.updateDescription(EasyMock.eq(extensionMock),
            EasyMock.eq(cmdLineMock));
      EasyMock.expectLastCall().once();
      registryServiceMock.updateServerInfo(EasyMock.eq(extensionMock),
            EasyMock.eq(cmdLineMock));
      EasyMock.expectLastCall().once();
      registryServiceMock.updateClientInfo(EasyMock.eq(extensionMock),
            EasyMock.eq(cmdLineMock));
      EasyMock.expectLastCall().once();
      registryServiceMock.updateTaskList(EasyMock.eq(extensionMock),
            EasyMock.eq(cmdLineMock));
      EasyMock.expectLastCall().once();
      registryServiceMock.updateFaultList(EasyMock.eq(extensionMock),
            EasyMock.eq(cmdLineMock));
      EasyMock.expectLastCall().once();
      registryServiceMock.updatePrivilegeList(EasyMock.eq(extensionMock),
            EasyMock.eq(cmdLineMock));
      EasyMock.expectLastCall().once();
      registryServiceMock.updateEventList(EasyMock.eq(extensionMock),
            EasyMock.eq(cmdLineMock));
      EasyMock.expectLastCall().once();
      registryServiceMock.updateResourceInfo(EasyMock.eq(extensionMock),
            EasyMock.eq(cmdLineMock));
      EasyMock.expectLastCall().once();
      registryServiceMock.updateTopLevelProperties(EasyMock.eq(extensionMock),
            EasyMock.eq(cmdLineMock));
      EasyMock.expectLastCall().once();
      registryServiceMock.updatelastHeartbeatTime(EasyMock.eq(extensionMock));
      EasyMock.expectLastCall().once();

      // expect register extension call
      vimPortMock.updateExtension(EasyMock.eq(extensionManagerMock),
            EasyMock.eq(extensionMock));
      EasyMock.expectLastCall().once();
      // expect connectionServiceMock calls to get vimPort and sessionManager
      EasyMock.replay(vimPortMock, connectionServiceMock, registryServiceMock,
            cmdLineMock, cmdLineParserMock);

      pluginCmd.execute(cmdLineArgsMock);

      EasyMock.verify(vimPortMock, connectionServiceMock, registryServiceMock,
            cmdLineMock, cmdLineParserMock);
   }

   @Test(expectedExceptions = RuntimeException.class)
   public void execute_whenTheExtensionDoesNotExist_throwsRuntimeException()
         throws Exception {
      // Expect extension is not found
      EasyMock.expect(vimPortMock
            .findExtension(EasyMock.anyObject(ManagedObjectReference.class),
                  EasyMock.eq(pluginKey))).andReturn(null).once();
      EasyMock.replay(vimPortMock);
      pluginCmd.execute(cmdLineArgsMock);
      EasyMock.verify(vimPortMock);
   }
}
