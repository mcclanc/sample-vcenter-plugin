/*
 * ******************************************************************
 * Copyright (c) 2022-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

package com.vmware.sample.remote.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import jakarta.xml.bind.DatatypeConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CertificateUtil {
   private static final Log _logger = LogFactory.getLog(
         CertificateUtil.class);

   private static final String[] ALGORITHM_NAMES = { "SHA-1", "SHA-224",
         "SHA-256", "SHA-384", "SHA-512" };

   /**
    * Checks whether the expected thumbprint matches the one generated
    * from the provided certificate.
    */
   public static boolean isThumbprintTrusted(final X509Certificate certificate,
         final String expectedThumbprint) {
      if (expectedThumbprint == null) {
         _logger.info("The expected thumbprint is null, so we allow " +
               "connection without verification.");
         return true;
      }
      for (final String algorithmName : ALGORITHM_NAMES) {
         // Extract thumbprint from certificate
         final MessageDigest md;
         try {
            md = MessageDigest.getInstance(algorithmName);
         } catch (final NoSuchAlgorithmException e) {
            _logger.error("Unrecognized algorithm: " + algorithmName);
            return false;
         }

         try {
            md.update(certificate.getEncoded());
         } catch (final CertificateEncodingException e) {
            _logger.error("Unable to get the encoded form of the certificate.");
            return false;
         }
         final String certThumb = DatatypeConverter.printHexBinary(md.digest())
               .toLowerCase();

         // Check match
         if (expectedThumbprint.equalsIgnoreCase(certThumb)) {
            return true;
         }
      }
      return false;
   }
}
