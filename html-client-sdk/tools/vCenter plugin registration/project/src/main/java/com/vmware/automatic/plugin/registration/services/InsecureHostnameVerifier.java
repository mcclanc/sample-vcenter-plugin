/*
 * ******************************************************************
 * Copyright (c) 2022-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.automatic.plugin.registration.services;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * An implementation of a SSL HostnameVerifier that is invoked as a last
 * instance of security if and only if the hostname verification of
 * the certificate fails.
 * <p>
 * In this case we'd like to provide the ability for skipping the hostname
 * verification, e.g. when in development mode.
 * <p>
 * By providing the '-insecure' command line argument the extension-registration
 * tool will make the hostname verification pass no matter what host
 * the tools is trying to connect to.
 * <p>
 * When not in '-insecure' mode the standard certificate hostname verification
 * will be performed. If it eventually fails the InsecureHostnameVerifier::verify
 * will also return false, which will make the hostname verification fail.
 */
public class InsecureHostnameVerifier implements HostnameVerifier {

   /**
    * The command line argument '-insecure' which is used for dev purposes
    * when no SSL validation is needed.
    */
   private final boolean insecure;

   InsecureHostnameVerifier(final boolean insecure) {
      this.insecure = insecure;
   }

   @Override
   public boolean verify(String s, SSLSession sslSession) {
      return insecure;
   }
}
