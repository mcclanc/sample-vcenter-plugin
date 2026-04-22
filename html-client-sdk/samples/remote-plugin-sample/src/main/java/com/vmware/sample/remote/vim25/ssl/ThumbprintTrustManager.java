/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote.vim25.ssl;

import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

import com.vmware.sample.remote.util.CertificateUtil;

/**
 * Implementation of {@link X509TrustManager} that checks certificates against a known
 * thumbprint (SHA-1 hash)
 */
public class ThumbprintTrustManager implements X509TrustManager {
   private final String expectedThumbprint;

   /**
    * Constructor
    *
    * @param expectedThumbprint SHA-1, or SHA-2 certificate thumbprint we
    *                           expect to receive from the host
    */
   public ThumbprintTrustManager(final String expectedThumbprint) {
      this.expectedThumbprint = expectedThumbprint;
   }

   @Override
   public void checkClientTrusted(final X509Certificate[] arg0,
         final String arg1) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void checkServerTrusted(final X509Certificate[] chain, final String authType) {
      if (!CertificateUtil.isThumbprintTrusted(chain[0], expectedThumbprint)) {
         throw new RuntimeException(
               "Host certificate thumbprint did not match expected value.");
      }
   }

   @Override
   public X509Certificate[] getAcceptedIssuers() {
      return null;
   }
}
