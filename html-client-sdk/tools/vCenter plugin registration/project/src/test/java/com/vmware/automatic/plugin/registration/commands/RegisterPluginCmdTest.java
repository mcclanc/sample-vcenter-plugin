/*
 * ******************************************************************
 * Copyright (c) 2020-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.automatic.plugin.registration.commands;

import com.vmware.vim25.Extension;
import org.apache.commons.cli.Options;
import org.easymock.EasyMock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RegisterPluginCmdTest extends PluginRegistryCmdTest {

   @BeforeMethod
   public void beforeEach() throws Exception {
      super.beforeEach();
      pluginCmd = new RegisterPluginCmd(connectionServiceMock,
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
            "Url where the plugin manifest will be downloaded from.", true, true,
            "plugin manifest url");
      // version
      assertOption(options, "v", "version", "Plugin version.", true, true,
            "version");
   }

   @Test
   public void execute() throws Exception {
      // expect registry service calls - this is the actual command behaviour
      registryServiceMock.updateDescription(EasyMock.anyObject(Extension.class),
            EasyMock.eq(cmdLineMock));
      EasyMock.expectLastCall().once();
      registryServiceMock.updateServerInfo(EasyMock.anyObject(Extension.class),
            EasyMock.eq(cmdLineMock));
      EasyMock.expectLastCall().once();
      registryServiceMock.updateClientInfo(EasyMock.anyObject(Extension.class),
            EasyMock.eq(cmdLineMock));
      EasyMock.expectLastCall().once();
      registryServiceMock
            .updateTaskList(EasyMock.anyObject(Extension.class),
                  EasyMock.eq(cmdLineMock));
      registryServiceMock
            .updateFaultList(EasyMock.anyObject(Extension.class),
                  EasyMock.eq(cmdLineMock));
      registryServiceMock
            .updatePrivilegeList(EasyMock.anyObject(Extension.class),
                  EasyMock.eq(cmdLineMock));
      registryServiceMock
            .updateEventList(EasyMock.anyObject(Extension.class),
                  EasyMock.eq(cmdLineMock));
      registryServiceMock
            .updateResourceInfo(EasyMock.anyObject(Extension.class),
                  EasyMock.eq(cmdLineMock));
      EasyMock.expectLastCall().once();
      registryServiceMock
            .updateTopLevelProperties(EasyMock.anyObject(Extension.class),
                  EasyMock.eq(cmdLineMock));
      EasyMock.expectLastCall().once();
      registryServiceMock
            .updatelastHeartbeatTime(EasyMock.anyObject(Extension.class));
      EasyMock.expectLastCall().once();

      // expect register extension call
      vimPortMock.registerExtension(EasyMock.eq(extensionManagerMock),
            EasyMock.anyObject(Extension.class));
      EasyMock.expectLastCall().once();

      EasyMock.replay(connectionServiceMock, registryServiceMock, cmdLineMock,
            cmdLineParserMock);

      // Trigger the real execution
      pluginCmd.execute(cmdLineArgsMock);

      EasyMock.verify(connectionServiceMock, registryServiceMock, cmdLineMock,
            cmdLineParserMock);
   }
}
