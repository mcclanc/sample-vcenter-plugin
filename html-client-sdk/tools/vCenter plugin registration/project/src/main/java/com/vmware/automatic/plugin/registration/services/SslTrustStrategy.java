/*
 * ******************************************************************
 * Copyright (c) 2016-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.automatic.plugin.registration.services;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * Trust strategy that does validate the server thumbprint (if provided), or
 * the certificate chain.
 */
public class SslTrustStrategy {

   /**
    * Based on the provided serverThumbprint decides whether to have a
    * Thumbprint Trust Strategy, or do a full certificate chain validation.
    *
    * @param insecure         - insecure vCenter Server connection (trust all strategy).
    * @param serverThumbprint - server's thumbprint.
    */
   void init(final boolean insecure, final String serverThumbprint) {
      try {
         SSLContext sc = SSLContext.getInstance("SSL");
         SSLSessionContext sslsc = sc.getServerSessionContext();
         sslsc.setSessionTimeout(0);
         sc.init(null, getTrustManager(insecure, serverThumbprint), null);
         HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
         HttpsURLConnection.setDefaultHostnameVerifier(
               new InsecureHostnameVerifier(insecure));
      } catch (NoSuchAlgorithmException | KeyManagementException e) {
         System.out.println("Error while creating SSLContext");
         throw new RuntimeException(e);
      }
   }

   /**
    * Based on whether an insecure connection is preferred, or a server thumbprint is provided as command line
    * argument we decide whether to trust all certificates, trust a server
    * certificate based on the provided thumbprint provided as cmd line argument,
    * or do full chain cert validation.
    *
    * @param serverThumbprint - the server's certificate thumbprint provided
    *                         as command line argument
    * @param insecure         - insecure vCenter Server connection
    */
   private TrustManager[] getTrustManager(final boolean insecure,
         final String serverThumbprint) {
      if (insecure) {
         return new TrustManager[] { new TrustAllCertificatesTrustManager() };
      } else {
         return serverThumbprint == null || serverThumbprint.length() == 0 ?
               null :
               new TrustManager[] {
                     new ThumbprintTrustManager(serverThumbprint) };
      }
   }
}
