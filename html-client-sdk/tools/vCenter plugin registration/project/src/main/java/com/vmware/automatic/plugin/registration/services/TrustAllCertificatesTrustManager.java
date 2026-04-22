/*
 * ******************************************************************
 * Copyright (c) 2022-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.automatic.plugin.registration.services;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * An implementation of a X509TrustManager that trusts all certificates.
 */
public class TrustAllCertificatesTrustManager implements X509TrustManager {
   @Override
   public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
         throws CertificateException {

   }

   @Override
   public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
         throws CertificateException {

   }

   @Override
   public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
   }
}
