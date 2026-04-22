/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.automatic.plugin.registration.services;

import javax.net.ssl.X509TrustManager;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Locale;

/**
 * Implementation of a custom TrustManager that extracts the thumbprint
 * from the server certificate and compares it to the provided value.
 */
public class ThumbprintTrustManager implements X509TrustManager {

   // Supported thumbprints algorithms
   private static final String[] ALGORITHM_NAMES = { "SHA-1", "SHA-256" };

   private final String expectedThumbprint;

   ThumbprintTrustManager(final String expectedThumbprint) {
      this.expectedThumbprint = expectedThumbprint.replaceAll("\\s+|:", "");
   }

   @Override
   public void checkClientTrusted(X509Certificate[] x509Certificates,
         String s) {
      // do nothing here
   }

   @Override
   public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
         throws CertificateException {
      for (final String algorithmName : ALGORITHM_NAMES) {
         // Extract thumbprint from certificate
         final MessageDigest md;
         try {
            md = MessageDigest.getInstance(algorithmName);
         } catch (final NoSuchAlgorithmException e) {
            throw new AssertionError(e);
         }

         md.update(x509Certificates[0].getEncoded());
         final String certThumb = hexify(md.digest());

         // Check match
         if (expectedThumbprint.equalsIgnoreCase(certThumb)) {
            return;
         }
      }

      throw new CertificateException(
            "Host certificate thumbprint did not match the provided value.");
   }

   @Override
   public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
   }

   private static String hexify(final byte bytes[]) {
      char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f' };
      StringBuffer buf = new StringBuffer(bytes.length * 2);
      for (int i = 0; i < bytes.length; ++i) {
         buf.append(hexDigits[(bytes[i] & 0xf0) >> 4]);
         buf.append(hexDigits[bytes[i] & 0x0f]);
      }
      return buf.toString().toUpperCase(Locale.ENGLISH);
   }
}
