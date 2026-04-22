/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote.configuration;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.env.Environment;

public class Configuration {

   private static final Log logger = LogFactory.getLog(Configuration.class);

   private static final String VCENTER_API_ENDPOINT_TEMPLATE = "https://%s:%s/sdk";
   private static final String VCENTER_REST_ENDPOINT_TEMPLATE = "https://%s:%s/api/ui";
   private static final String PROPERTY_KEY_FQDN = "vcenter.fqdn";
   private static final String PROPERTY_KEY_PORT = "vcenter.port";
   private static final String PROPERTY_KEY_GUID = "vcenter.guid";
   private static final String PROPERTY_KEY_THUMBPRINT = "vcenter.thumbprint";
   private static final String MISSING_PROPERTY_VALIDATION_MSG =
         "vCenter Server property %s must not be null or empty!";
   private static final String THUMBPRINT_HEX_VALIDATION_MSG =
         "The provided thumbprint is not in hexadecimal format";
   private static final String THUMBPRINT_LENGTH_VALIDATION_MSG =
         "The length of the provided "
               + "thumbprint does not match any of the lengths of the known "
               + "certificate key algorithms (SHA-1 40 chars, SHA-224 56 chars, "
               + "SHA-256 64 chars, SHA-384 96 chars, SHA-512 128 chars).";
   private static final String THUMBPRINT_DELIMITERS_REGEX = "[: ]";
   private static final String THUMBPRINT_HEX_REGEX = "[0-9a-fA-F]+";
   // Algorithm lengths to validate for:
   // SHA-1   40  chars (160 bits)
   // SHA-224 56  chars (224 bits)
   // SHA-256 64  chars (256 bits)
   // SHA-384 96  chars (384 bits)
   // SHA-512 128 chars (512 bits)
   private static final String THUMBPRINT_KNOWN_LENGTHS_REGEX =
         "\\b.{40}\\b|\\b.{56}\\b|\\b.{64}\\b|\\b.{96}\\b|\\b.{128}\\b";

   private final Environment env;

   public Configuration(Environment env) {
      this.env = env;
   }

   public void initialize() {
      Validate.notEmpty(env.getProperty(PROPERTY_KEY_FQDN),
            String.format(MISSING_PROPERTY_VALIDATION_MSG, PROPERTY_KEY_FQDN));
      Validate.notEmpty(env.getProperty(PROPERTY_KEY_GUID),
            String.format(MISSING_PROPERTY_VALIDATION_MSG, PROPERTY_KEY_GUID));
      Validate.notEmpty(env.getProperty(PROPERTY_KEY_THUMBPRINT),
            String.format(MISSING_PROPERTY_VALIDATION_MSG, PROPERTY_KEY_THUMBPRINT));
      Validate.notEmpty(env.getProperty(PROPERTY_KEY_PORT),
            String.format(MISSING_PROPERTY_VALIDATION_MSG, PROPERTY_KEY_PORT));

      // Validate the provided thumbprint is hexadecimal.
      Validate.isTrue(getVcenterSslThumbprint().matches(THUMBPRINT_HEX_REGEX),
            THUMBPRINT_HEX_VALIDATION_MSG);

      // Validate thumbprint's length against the known SHA-1 and SHA-2s
      Validate.isTrue(
            getVcenterSslThumbprint().matches(THUMBPRINT_KNOWN_LENGTHS_REGEX),
            THUMBPRINT_LENGTH_VALIDATION_MSG);

      logger.info(
            String.format("Remote Plugin was initialized with" + System.lineSeparator() +
                        "vCenter Server FQDN: %s" + System.lineSeparator() +
                        "vCenter Server GUID: %s" + System.lineSeparator() +
                        "vCenter Server Thumbprint: %s" + System.lineSeparator() +
                        "vCenter Server Port: %s",
                  getVcenterServerFqdn(), getVcenterGuid(),
                  getVcenterSslThumbprint(), getVcenterServerPort()));
   }

   public String getVcenterServerFqdn() {
      return env.getProperty(PROPERTY_KEY_FQDN);
   }

   public String getVcenterGuid() {
      return env.getProperty(PROPERTY_KEY_GUID);
   }

   public String getVcenterSslThumbprint() {
      return replaceThumbprintDelimiters(env.getProperty(
            PROPERTY_KEY_THUMBPRINT));
   }

   public URI getVcenterApiEndpoint() throws URISyntaxException {
      return new URI(
            String.format(VCENTER_API_ENDPOINT_TEMPLATE, getVcenterServerFqdn(),
                  getVcenterServerPort()));
   }

   public String getVcenterRestEndpoint() {
      return String.format(VCENTER_REST_ENDPOINT_TEMPLATE, getVcenterServerFqdn(),
            getVcenterServerPort());
   }

   public String getVcenterServerPort() {
      return env.getProperty(PROPERTY_KEY_PORT);
   }

   private String replaceThumbprintDelimiters(final String thumbprint) {
      return thumbprint.replaceAll(THUMBPRINT_DELIMITERS_REGEX, "");
   }
}
