/*
 * ******************************************************************
 * Copyright (c) 2016-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.automatic.plugin.registration.services;

import com.vmware.vim25.InvalidLocaleFaultMsg;
import com.vmware.vim25.InvalidLoginFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VimService;
import org.apache.commons.cli.CommandLine;

import javax.net.ssl.SSLHandshakeException;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;
import org.apache.commons.lang3.StringUtils;

import java.io.Console;
import java.io.IOException;
import java.util.Map;

/**
 * Service that provides public methods to connect/disconnect to/from
 * a vCenter Server.
 */
public class ConnectionService {

   /**
    * Service instance ref
    */
   private static final ManagedObjectReference SVC_INST_REF = new ManagedObjectReference();

   /**
    * Name and Type of Service instance
    */
   private static final String SVC_INST_NAME = "ServiceInstance";
   private final VimService vimService;
   private final SslTrustStrategy trustStrategy;

   private VimPortType vimPort;
   private ServiceContent serviceContent;
   private ManagedObjectReference extensionManager;
   private boolean isConnected;

   public ConnectionService(VimService vimService,
         SslTrustStrategy trustStrategy) {
      this.vimService = vimService;
      this.trustStrategy = trustStrategy;
   }

   /**
    * Creates session with the vCenter Server
    *
    * @param commandLine - command line arguments
    */
   public void connect(CommandLine commandLine) {
      if (isConnected) {
         return;
      }
      final String username = commandLine.getOptionValue("u");
      String password = commandLine.getOptionValue("p");
      if (password == null || password.length() == 0) {
         password = readPasswordFromStdin();
      }
      String url = commandLine.getOptionValue("url");
      if (StringUtils.isBlank(url)) {
         throw new RuntimeException("vCenter Server url cannot be blank. Please provide a vCenter Server url in the format \"<vc-fqdn>/sdk\"");
      }
      if (!(url.endsWith("/sdk") || url.endsWith("/sdk/"))) {
         if (url.endsWith("/")) {
            url = url.concat("sdk");
         } else {
            url = url.concat("/sdk");
         }
      }
      final String vcServerThumbprint = ThumbprintConverter.convertThumbprintToColonSeparated(
            commandLine.getOptionValue("vct"));
      final boolean insecure = commandLine.hasOption("insecure")
            || Boolean.parseBoolean(
                  System.getenv("EXTENSION_REGISTRATION_INSECURE"))
            || Boolean.parseBoolean(
                  System.getProperty("extension.registration.insecure"));
      try {
         trustStrategy.init(insecure, vcServerThumbprint);
         SVC_INST_REF.setType(SVC_INST_NAME);
         SVC_INST_REF.setValue(SVC_INST_NAME);

         vimPort = vimService.getVimPort();
         Map<String, Object> ctxt = ((BindingProvider) vimPort)
               .getRequestContext();

         ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
         ctxt.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

         serviceContent = vimPort.retrieveServiceContent(SVC_INST_REF);
         vimPort.login(serviceContent.getSessionManager(), username, password,
               null);

         extensionManager = serviceContent.getExtensionManager();
         isConnected = true;
      } catch (RuntimeFaultFaultMsg | InvalidLocaleFaultMsg | InvalidLoginFaultMsg e) {
         final String msg = String.format(
               "Error while connecting to vCenter Server SDK <%s>. Please verify -url, -username, and password (-p or stdin); password value is not included in this message.",
               url);
         throw new RuntimeException(msg, e);
      } catch (WebServiceException e) {
         if (e.getCause() != null && e
               .getCause() instanceof SSLHandshakeException) {
            final String msg = String.format(
                  "Error while connecting to vCenter Server SDK <%s>. "
                        + "\nThis is because your vCenter Server has no valid certificate from a trusted authority. "
                        + "\nTo be able to securely connect to your vCenter server, please provide the server's SHA-1, "
                        + "or SHA-256 thumbprint as command line argument '-vct'."
                        + "\nIf you want to skip the security checks, use the '-insecure' command line parameter instead.",
                  url);
            throw new RuntimeException(msg);
         } else if (e.getCause() != null && e
               .getCause() instanceof IOException) {
            final String msg = String.format(
                  "Cannot securely connect to vCenter Server SDK <%s>."
                        + "\nYou probably try to refer your vCenter Server by IP address and it is not listed as a subject alternative name in the server's certificate."
                        + "\nPlease, try to refer your vCenter Server by its FQDN, not IP address."
                        + "\nIf you want to skip the security checks, use the '-insecure' command line parameter instead.",
                  url);
            throw new RuntimeException(msg, e);
         } else {
            throw e;
         }
      }
   }

   /**
    * Asks the user to type his vCenter's user credentials.
    */
   private String readPasswordFromStdin() {
      final Console console = System.console();
      // There may be no console, e.g. if running from an editor like Eclipse.
      // Then warn to provide the password via cmd line argument '-p'.
      if (console != null) {
         final char[] password;
         password = console.readPassword("Password: ");
         return String.valueOf(password);
      } else {
         throw new IllegalStateException("No console detected. "
               + "Please provide your password via cmd line argument '-p'.");
      }
   }

   /**
    * Destroys session to vCenter Server.
    *
    * @throws RuntimeFaultFaultMsg
    */
   public void disconnect(CommandLine commandLine) {
      if (!isConnected) {
         return;
      }
      final String url = commandLine.getOptionValue("url");
      try {
         getVimPort().logout(serviceContent.getSessionManager());
      } catch (RuntimeFaultFaultMsg e) {
         final String msg = String.format(
               "Error while disconnecting from vCenter Server SDK <%s>.", url);
         throw new RuntimeException(msg, e);
      } finally {
         isConnected = false;
      }
   }

   /**
    * Get VimPortType instance
    */
   public VimPortType getVimPort() {
      if (vimPort == null) {
         throw new RuntimeException(
               "No Connection to vCenter Server established");
      }
      return vimPort;
   }

   /**
    * Get extension manager instance.
    */
   public ManagedObjectReference getExtensionManager() {
      if (extensionManager == null) {
         throw new RuntimeException(
               "No connection to vCenter Server established.");
      }
      return extensionManager;
   }
}
