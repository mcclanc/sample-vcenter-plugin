/*
 * ******************************************************************
 * Copyright (c) 2022-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.automatic.plugin.registration.services;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import java.security.SecureRandom;

import static org.mockito.Mockito.times;

public class SslTrustStrategyTest {

   private SslTrustStrategy trustManager;
   private SSLContext sslContextMock;
   private SSLSessionContext sslSessionContextMock;
   private SSLSocketFactory sslSocketFactoryMock;

   @BeforeMethod
   public void init() {
      trustManager = new SslTrustStrategy();
      sslContextMock = Mockito.mock(SSLContext.class);
      sslSessionContextMock = Mockito.mock(SSLSessionContext.class);
      sslSocketFactoryMock = Mockito.mock(SSLSocketFactory.class);
   }

   @Test
   public void trustAll() throws Exception {
      // Set mock expectations
      MockedStatic<SSLContext> sslContextMockedStatic = Mockito.mockStatic(
            SSLContext.class);
      sslContextMockedStatic.when( () -> SSLContext.getInstance("SSL")).thenReturn(sslContextMock);
      Mockito.when(sslContextMock.getServerSessionContext()).thenReturn(sslSessionContextMock);
      Mockito.when(sslContextMock.getSocketFactory()).thenReturn(sslSocketFactoryMock);
      Mockito.doNothing().when(sslSessionContextMock).setSessionTimeout(Mockito.isA(Integer.class));

      trustManager.init(true, "vc-server-thumbprint");

      // Verify method invocations
      sslContextMockedStatic.verify( () -> SSLContext.getInstance("SSL"), times(1));
      SSLContext.getInstance("SSL");
      Mockito.verify(sslContextMock, times(1))
            .getServerSessionContext();
      Mockito.verify(sslSessionContextMock, times(1))
            .setSessionTimeout(0);
      Mockito.verify(sslContextMock, times(1))
            .init(Mockito.isNull(KeyManager[].class),
                  Mockito.any(TrustManager[].class),
                  Mockito.isNull(SecureRandom.class));
   }

}
