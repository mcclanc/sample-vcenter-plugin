/*
 * ******************************************************************
 * Copyright (c) 2022-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.automatic.plugin.registration.services;

import com.vmware.automatic.plugin.registration.resources.CommandLineBuilder;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.UserSession;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VimService;
import org.apache.commons.cli.CommandLine;
import org.easymock.EasyMock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import jakarta.xml.ws.BindingProvider;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertNotNull;

public class ConnectionServiceTest {

   private ConnectionService connectionService;
   private VimService vimServiceMock;
   private SslTrustStrategy sslTrustStrategy;
   private IVimPortCastMock vimPortTypeMock;
   private ServiceContent serviceContentMock;
   private ManagedObjectReference extensionManagerMock;
   private ManagedObjectReference sessionManagerMock;
   private Map<String, Object> contextMapMock;

   @BeforeMethod
   public void init() throws Exception {
      vimServiceMock = EasyMock.createMock(VimService.class);
      sslTrustStrategy = EasyMock.createMock(SslTrustStrategy.class);
      connectionService = new ConnectionService(vimServiceMock,
            sslTrustStrategy);
      vimPortTypeMock = EasyMock.createMock(IVimPortCastMock.class);
      serviceContentMock = EasyMock.createMock(ServiceContent.class);
      extensionManagerMock = EasyMock.createMock(ManagedObjectReference.class);
      sessionManagerMock = EasyMock.createMock(ManagedObjectReference.class);
      contextMapMock = EasyMock.createMock(HashMap.class);

      sslTrustStrategy.init(false, "vc-server-thumbprint");
      EasyMock.expectLastCall().once();
      EasyMock.expect(vimServiceMock.getVimPort()).andReturn(vimPortTypeMock)
            .once();
      EasyMock.expect(vimPortTypeMock.retrieveServiceContent(
            EasyMock.anyObject(ManagedObjectReference.class)))
            .andReturn(serviceContentMock).once();
      EasyMock.expect(vimPortTypeMock.getRequestContext())
            .andReturn(contextMapMock).once();
      EasyMock.expect(contextMapMock
            .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                  "https://my-vcenter-server.com/sdk"))
            .andReturn("https://my-vcenter-server.com/sdk").once();
      EasyMock.expect(
            contextMapMock.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true))
            .andReturn(true).once();
      EasyMock.expect(serviceContentMock.getExtensionManager())
            .andReturn(extensionManagerMock).once();
      EasyMock.expect(serviceContentMock.getSessionManager())
            .andReturn(sessionManagerMock).once();
      EasyMock.expect(vimPortTypeMock
            .login(sessionManagerMock, "administrator@vsphere.local",
                  "TEST_MOCK_VCENTER_PASSWORD", null))
            .andReturn(EasyMock.createMock(UserSession.class)).once();
      EasyMock.replay(vimServiceMock, vimPortTypeMock, serviceContentMock,
            contextMapMock, extensionManagerMock, sessionManagerMock,
            sslTrustStrategy);
   }

   @Test
   public void connect() throws Exception {
      final CommandLine commandLine = new CommandLineBuilder().defaults()
            .vcServerThumbprint("vc-server-thumbprint")
            .build();
      // Verify connection is established
      connectionService.connect(commandLine);
      assertNotNull(connectionService.getExtensionManager());
      assertNotNull(connectionService.getVimPort());

      // Verify service does not connect twice
      connectionService.connect(commandLine);

      // Verify all mock calls
      EasyMock.verify(vimServiceMock, vimPortTypeMock, serviceContentMock,
            contextMapMock, sslTrustStrategy, sessionManagerMock,
            extensionManagerMock);
   }

   @Test
   public void disconnect() throws Exception {
      // Connect first
      final CommandLine commandLine = new CommandLineBuilder().defaults()
            .vcServerThumbprint("vc-server-thumbprint")
            .build();
      // Verify connection is established
      connectionService.connect(commandLine);

      // Verify all mock calls
      EasyMock.verify(vimServiceMock, vimPortTypeMock, serviceContentMock,
            contextMapMock, sslTrustStrategy, sessionManagerMock,
            extensionManagerMock);

      // Reset mocks and set new expectations
      EasyMock.reset(vimPortTypeMock, serviceContentMock, sessionManagerMock);

      // Set new expectations for disconnect() behaviour
      EasyMock.expect(serviceContentMock.getSessionManager())
            .andReturn(sessionManagerMock).once();
      vimPortTypeMock.logout(sessionManagerMock);
      EasyMock.expectLastCall();
      EasyMock.replay(vimPortTypeMock, serviceContentMock, sessionManagerMock);

      connectionService.disconnect(commandLine);
      EasyMock.verify(vimPortTypeMock, serviceContentMock, sessionManagerMock);

      // Verify service does not disconnects twice
      connectionService.disconnect(commandLine);
      EasyMock.verify(vimServiceMock, serviceContentMock, sessionManagerMock);
   }

   @Test(expectedExceptions = RuntimeException.class)
   public void getExtensionManager_whenNoConnectionEstablished_throwsException() {
      connectionService.getExtensionManager();
   }

   @Test(expectedExceptions = RuntimeException.class)
   public void getVimPort_whenNoConnectionEstablished_throwsException() {
      connectionService.getVimPort();
   }

}

interface IVimPortCastMock extends BindingProvider, VimPortType {

}
