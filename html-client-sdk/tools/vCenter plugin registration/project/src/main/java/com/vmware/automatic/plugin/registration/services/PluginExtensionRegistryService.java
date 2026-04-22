/*
 * ******************************************************************
 * Copyright (c) 2016-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.automatic.plugin.registration.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.vmware.vim25.Description;
import com.vmware.vim25.Extension;
import com.vmware.vim25.ExtensionClientInfo;
import com.vmware.vim25.ExtensionEventTypeInfo;
import com.vmware.vim25.ExtensionFaultTypeInfo;
import com.vmware.vim25.ExtensionPrivilegeInfo;
import com.vmware.vim25.ExtensionResourceInfo;
import com.vmware.vim25.ExtensionServerInfo;
import com.vmware.vim25.ExtensionTaskTypeInfo;
import com.vmware.vim25.KeyValue;

import org.apache.commons.cli.CommandLine;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * A service that provides public methods for update of pluginExtension's state.
 */
public class PluginExtensionRegistryService {
   /**
    * Extension types
    */
   private static final String REMOTE_PLUGIN_TYPE = "vsphere-client-remote";

   /**
    * Extension server protocols
    */
   private static final String HTTPS_PROTOCOL = "HTTPS";
   private static final String HTTP_PROTOCOL = "HTTP";

   private static final String MANIFEST_SERVER_TYPE = "MANIFEST_SERVER";
   private static final String DEFAULT_SERVER_TYPE = "PLUGIN_SERVER";

   /**
    * Extension server adminEmail
    */
   private static final String ADMIN_EMAIL = "noreply@vmware.com";

   private static String readCertificate(final String certificateFilePathString)
         throws CertificateException, IOException {
      if (certificateFilePathString == null || certificateFilePathString.isEmpty()) {
         return certificateFilePathString;
      }

      final Path certificateAbsoluteFilePath = Paths.get(certificateFilePathString).toAbsolutePath();
      final InputStream certificateFileInputStream = new FileInputStream(certificateAbsoluteFilePath.toString());
      final String certificatePemPreEncapsulationBoundary = "-----BEGIN CERTIFICATE-----";
      final String certificatePemPostEncapsulationBoundary = "-----END CERTIFICATE-----";
      final Character certificatePemEolCharacter = '\n';

      final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
      final X509Certificate certificate = (X509Certificate) certificateFactory
            .generateCertificate(certificateFileInputStream);

      // Encode the certificate in PEM strict format as per RFC 7468, Section 3 (https://www.rfc-editor.org/rfc/rfc7468#section-3)
      final String certificateBase64EncodedData =
            Base64.getEncoder().encodeToString(certificate.getEncoded());
      final StringBuilder certificatePemEncodedDataBuffer = new StringBuilder();
      certificatePemEncodedDataBuffer
            .append(certificatePemPreEncapsulationBoundary)
            .append(certificatePemEolCharacter);
      for (int x = 0; x < certificateBase64EncodedData.length(); x++) {
         if ((x > 0) && (x % 64 == 0)) {
            certificatePemEncodedDataBuffer.append(certificatePemEolCharacter);
         }
         certificatePemEncodedDataBuffer.append(certificateBase64EncodedData.charAt(x));
      }
      certificatePemEncodedDataBuffer
            .append(certificatePemEolCharacter)
            .append(certificatePemPostEncapsulationBoundary)
            .append(certificatePemEolCharacter);

      return certificatePemEncodedDataBuffer.toString();
   }

   final Gson gson;

   public PluginExtensionRegistryService() {
      gson = new Gson();
   }

   /**
    * Updates top level properties of the given extension instance.
    *
    * @param extension - current extension's instance
    * @param commandLine - command line arguments
    */
   public void updateTopLevelProperties(final Extension extension,
         CommandLine commandLine) {
      final String version = commandLine.getOptionValue("v");
      final String company = commandLine.getOptionValue("c");

      // Set the key if extension is new
      if (extension.getKey() == null) {
         extension.setKey(commandLine.getOptionValue("k"));
      }

      extension
            .setVersion((version != null) ? version : extension.getVersion());
      extension
            .setCompany(company != null ? company : extension.getCompany());
   }

   /**
    * Updates extension's description.
    *
    * @param extension - current extension's instance
    * @param commandLine - command line arguments
    */
   public void updateDescription(final Extension extension,
         final CommandLine commandLine) {
      final String name = commandLine.getOptionValue("n");
      final String summary = commandLine.getOptionValue("s");
      final Description description;
      if (extension.getDescription() != null) {
         description = extension.getDescription();
      } else {
         description = new Description();
         description.setLabel("");
         description.setSummary("");
      }
      description.setLabel((name != null) ? name : description.getLabel());
      description
            .setSummary((summary != null) ? summary : description.getSummary());
      extension.setDescription(description);
   }

   /**
    * Updates the current extension's client info, or creates a new one,
    * if missing and any clientInfo's attributes are provided.
    *
    * @param extension - current extension's instance.
    * @param commandLine - command line arguments
    */
   public void updateClientInfo(Extension extension, CommandLine commandLine) {
      final String version = commandLine.getOptionValue("v");
      final String company = commandLine.getOptionValue("c");
      final String pluginUrl = commandLine.getOptionValue("pu");

      final ExtensionClientInfo extClientInfo;
      if (extension.getClient().size() > 0) {
         extClientInfo = extension.getClient().get(0);
      } else {
         // Create new ExtensionClientInfo instance and set the not nullables
         extClientInfo = new ExtensionClientInfo();
         extClientInfo.setCompany("");
      }
      extClientInfo.setVersion(
            (version != null) ? version : extClientInfo.getVersion());
      extClientInfo.setCompany(
            (company != null) ? company : extClientInfo.getCompany());
      extClientInfo.setDescription(extension.getDescription());
      extClientInfo.setType(REMOTE_PLUGIN_TYPE);
      extClientInfo.setUrl(pluginUrl != null ? pluginUrl : extClientInfo.getUrl());
      if (extension.getClient().size() == 0) {
         extension.getClient().add(extClientInfo);
      } else {
         extension.getClient().set(0, extClientInfo);
      }
   }

   /**
    * Updates the current extension's task list,
    * with tasks from a file which path is given with the 'taskList' option.
    * The format of the file is:
    * [
    *    {
    *       "taskId": string
    *    },
    *    ...
    * ]
    *
    * @param extension - current extension's instance
    * @param commandLine - command line arguments
    */
   public void updateTaskList(Extension extension, CommandLine commandLine) throws
         FileNotFoundException {
      final String taskListFilePath = commandLine.getOptionValue("taskList");
      if (taskListFilePath == null) {
         return;
      }

      JsonObject[] taskInfos = gson.fromJson(
            new BufferedReader(
                  new InputStreamReader(new FileInputStream(taskListFilePath),
                        StandardCharsets.UTF_8)), JsonObject[].class);

      List<ExtensionTaskTypeInfo> tempExtensionInfos = new ArrayList<>(taskInfos.length);
      for (JsonObject taskInfo : taskInfos) {
         ExtensionTaskTypeInfo extensionTaskInfo = new ExtensionTaskTypeInfo();
         JsonElement taskIdObj = taskInfo.get("taskId");
         if (taskIdObj == null) {
            throw new RuntimeException(
                  "Missing property 'taskId' in task object: " + gson.toJson(taskInfo));
         }
         extensionTaskInfo.setTaskID(taskIdObj.getAsString());
         tempExtensionInfos.add(extensionTaskInfo);
      }

      List<ExtensionTaskTypeInfo> extensionInfos = extension.getTaskList();
      extensionInfos.clear();
      extensionInfos.addAll(tempExtensionInfos);
   }

   /**
    * Updates the current extension's fault list,
    * with faults from a file which path is given with the 'faultList' option.
    * The format of the file is:
    * [
    *    {
    *       "faultId": string
 *       },
    *    ...
    * ]
    *
    * @param extension - current extension's instance
    * @param commandLine - command line arguments
    */
   public void updateFaultList(Extension extension, CommandLine commandLine) throws
         FileNotFoundException {
      final String faultListFilePath = commandLine.getOptionValue("faultList");
      if (faultListFilePath == null) {
         return;
      }

      JsonObject[] faultInfos = gson.fromJson(
            new BufferedReader(
                  new InputStreamReader(new FileInputStream(faultListFilePath),
                        StandardCharsets.UTF_8)), JsonObject[].class);

      List<ExtensionFaultTypeInfo> tempExtensionInfos = new ArrayList<>(faultInfos.length);
      for (JsonObject faultInfo : faultInfos) {
         ExtensionFaultTypeInfo extensionFaultInfo = new ExtensionFaultTypeInfo();
         JsonElement faultIdObj = faultInfo.get("faultId");
         if (faultIdObj == null) {
            throw new RuntimeException("Missing property 'faultId' in fault object: " +
                  gson.toJson(faultInfo));
         }
         extensionFaultInfo.setFaultID(faultInfo.get("faultId").getAsString());
         tempExtensionInfos.add(extensionFaultInfo);
      }

      List<ExtensionFaultTypeInfo> extensionInfos = extension.getFaultList();
      extensionInfos.clear();
      extensionInfos.addAll(tempExtensionInfos);
   }

   /**
    * Updates the current extension's privilege list,
    * with privileges from a file which path is given with the 'privilegeList' option.
    * The format of the file is:
    * [
    *    {
    *       "groupId": string,
    *       "privileges": [
    *          {
    *             "privilegeId": string
    *          },
    *          ...
    *       ]
    *    },
    *    ...
    * ]
    *
    * @param extension - current extension's instance
    * @param commandLine - command line arguments
    */
   public void updatePrivilegeList(Extension extension, CommandLine commandLine) throws
         FileNotFoundException {
      final String privilegeListFilePath = commandLine.getOptionValue("privilegeList");
      if (privilegeListFilePath == null) {
         return;
      }

      JsonObject[] privilegeGroupInfos = gson.fromJson(
            new BufferedReader(
                  new InputStreamReader(new FileInputStream(privilegeListFilePath),
                        StandardCharsets.UTF_8)), JsonObject[].class);

      List<ExtensionPrivilegeInfo> tempExtensionInfos = new ArrayList<>(privilegeGroupInfos.length);
      for (JsonObject privilegeGroupInfo : privilegeGroupInfos) {
         JsonArray privileges = privilegeGroupInfo.getAsJsonArray("privileges");
         if (privileges == null) {
            throw new RuntimeException(
                  "Missing property 'privileges' in privilege object: " +
                        gson.toJson(privilegeGroupInfo));
         }

         final JsonElement groupIdObj = privilegeGroupInfo.get("groupId");
         if (groupIdObj == null) {
            throw new RuntimeException(
                  "Missing property 'groupId' in privilege object: " +
                        gson.toJson(privilegeGroupInfo));
         }
         String groupId = groupIdObj.getAsString();

         for (JsonElement privilegeInfo : privileges) {
            JsonElement privlegeIdObj = privilegeInfo.getAsJsonObject().get(
                  "privilegeId");
            if (privlegeIdObj == null) {
               throw new RuntimeException(
                     "Missing property 'privilegeId' in privilege object: " +
                           gson.toJson(privilegeGroupInfo));
            }
            ExtensionPrivilegeInfo extensionPrivilegeInfo = new ExtensionPrivilegeInfo();
            extensionPrivilegeInfo.setPrivGroupName(groupId);
            extensionPrivilegeInfo.setPrivID(
                  groupId + "." + privlegeIdObj.getAsString());
            tempExtensionInfos.add(extensionPrivilegeInfo);
         }
      }

      List<ExtensionPrivilegeInfo> extensionInfos = extension.getPrivilegeList();
      extensionInfos.clear();
      extensionInfos.addAll(tempExtensionInfos);
   }

   /**
    * Updates the current extension's event list with events from a file
    * which path is given with the 'eventList' option.
    * The format of the file is:
    * [
    *    {
    *       "eventId": string,
    *       "eventTypeSchema": string
    *    },
    *    ...
    * ]
    *
    * @param extension - current extension's instance
    * @param commandLine - command line arguments
    */
   public void updateEventList(final Extension extension,
         final CommandLine commandLine) throws FileNotFoundException {
      final String eventListFilePath = commandLine.getOptionValue("eventList");
      if (eventListFilePath == null) {
         return;
      }

      final JsonObject[] eventInfos = gson.fromJson(
            new BufferedReader(
                  new InputStreamReader(new FileInputStream(eventListFilePath),
                        StandardCharsets.UTF_8)), JsonObject[].class);

      final List<ExtensionEventTypeInfo> tempExtensionInfos = new ArrayList<>(eventInfos.length);
      for (final JsonObject eventInfo : eventInfos) {
         final JsonElement eventIdObj = eventInfo.get("eventId");
         if (eventIdObj == null) {
            throw new RuntimeException(
                  "Missing property 'eventId' in event object: " +
                        gson.toJson(eventInfo));
         }
         final ExtensionEventTypeInfo extensionEventTypeInfo = new ExtensionEventTypeInfo();
         extensionEventTypeInfo.setEventID(eventIdObj.getAsString());

         final JsonElement eventTypeSchemaObj = eventInfo.get("eventTypeSchema");
         if (eventTypeSchemaObj != null) {
            extensionEventTypeInfo.setEventTypeSchema(eventTypeSchemaObj.getAsString());
         }
         tempExtensionInfos.add(extensionEventTypeInfo);
      }

      final List<ExtensionEventTypeInfo> extensionInfos = extension.getEventList();
      extensionInfos.clear();
      extensionInfos.addAll(tempExtensionInfos);
   }

   /**
    * Updates the current extension's resource list,
    * with resources from a file which path is given with the 'resourceList' option.
    * The format of the file is:
    * {
    *    "locale": {
    *       "msgKey": "msgValue",
    *       ...
 *       },
    *    ...
    * }
    *
    * @param extension - current extension's instance
    * @param commandLine - command line arguments
    */
   public void updateResourceInfo(Extension extension, CommandLine commandLine) throws
         FileNotFoundException {
      final String taskListFilePath = commandLine.getOptionValue("resourceList");
      if (taskListFilePath == null) {
         return;
      }

      JsonObject resourceInfos = gson.fromJson(
            new BufferedReader(
                  new InputStreamReader(new FileInputStream(taskListFilePath),
                        StandardCharsets.UTF_8)), JsonObject.class);

      List<ExtensionResourceInfo> tempExtensionInfos = new ArrayList<>(resourceInfos.size());
      for (Map.Entry<String, JsonElement> resourcesByLocale : resourceInfos.entrySet()) {
         ExtensionResourceInfo extResourceInfo = new ExtensionResourceInfo();
         extResourceInfo.setLocale(resourcesByLocale.getKey());
         extResourceInfo.setModule("resources");
         for (Map.Entry<String, JsonElement> resource : resourcesByLocale.getValue().getAsJsonObject().entrySet()) {
            final KeyValue kvPair = new KeyValue();
            kvPair.setKey(resource.getKey());
            kvPair.setValue(resource.getValue().getAsString());
            extResourceInfo.getData().add(kvPair);
         }
         tempExtensionInfos.add(extResourceInfo);
      }

      List<ExtensionResourceInfo> extensionInfos = extension.getResourceList();
      extensionInfos.clear();
      extensionInfos.addAll(tempExtensionInfos);
   }

   /**
    * Updates the current extension's server infos, or creates new ones.
    *
    * @param extension - the current extension's instance
    * @param commandLine - command line arguments
    */
   public void updateServerInfo(Extension extension, CommandLine commandLine) {
      final String pluginUrl = commandLine.getOptionValue("pu");
      final String serverThumbprint = ThumbprintConverter.convertThumbprintToColonSeparated(
            commandLine.getOptionValue("st"));
      final String serverCertificatePem;
      try {
         serverCertificatePem = readCertificate(commandLine.getOptionValue("scf"));
      } catch (IOException | CertificateException e) {
            throw new IllegalArgumentException("Could not read the certificate file" +
                  " specified by the \"serverCertificateFile\" parameter.", e);
      }

      final String company = commandLine.getOptionValue("c");
      final String additionalPluginServers = commandLine.getOptionValue("ps");
      final ServerInfo[] serverInfos;
      if (additionalPluginServers != null) {
         serverInfos = gson.fromJson(additionalPluginServers, ServerInfo[].class);
      } else {
         serverInfos = null;
      }
      if (isPluginUrlHttp(pluginUrl)) {
         System.out.println(
               "INFO: Not using https for your plugin URL is OK for testing but not recommended for production."
                     + "\nUsers will have to include the flag allowHttp=true in their vSphere Client webclient.properties otherwise the http URL will be ignored");
      }
      // create/update the server info for the plugin server hosting the plugin manifest
      ExtensionServerInfo manifestServerInfo;
      final List<ExtensionServerInfo> servers = extension.getServer();
      // update the manifest server info as older registrations may contain
      // type HTTPS for the manifest server info instead of MANIFEST_SERVER
      if (!servers.isEmpty()) {
         manifestServerInfo = servers.get(0);
      } else {
         manifestServerInfo = new ExtensionServerInfo();
         manifestServerInfo.getAdminEmail().add(ADMIN_EMAIL);
         manifestServerInfo.setCompany("");
      }
      manifestServerInfo.setType(MANIFEST_SERVER_TYPE);
      manifestServerInfo.setDescription(extension.getDescription());

      /*
      There is the following constraint regarding the "serverThumbprint" and
      the "serverCertificate" fields of an ExtensionServerInfo:
      If both the "serverThumbprint" and the "serverCertificate" fields are set
      then the (calculated) thumbprint of the "serverCertificate" field
      MUST match the thumbprint stored in the "serverThumbprint" field.
      */
      manifestServerInfo.setServerThumbprint(serverThumbprint != null ?
            serverThumbprint : manifestServerInfo.getServerThumbprint());
      manifestServerInfo.setServerCertificate(serverCertificatePem != null ?
            serverCertificatePem : manifestServerInfo.getServerCertificate());

      manifestServerInfo.setUrl(
            pluginUrl != null ? pluginUrl : manifestServerInfo.getUrl());
      manifestServerInfo.setCompany(
            company != null ? company : manifestServerInfo.getCompany());

      final List<ExtensionServerInfo> previousServerInfos = new ArrayList<>(servers);

      // we'll recalculate the server infos
      servers.clear();
      // the first server info is always the manifest server info
      servers.add(manifestServerInfo);

      if (serverInfos == null || serverInfos.length == 0) {
         // plugin servers were not updated - preserve previous ones
         for (int i = 1; i < previousServerInfos.size(); ++i) {
            servers.add(previousServerInfos.get(i));
         }
         return;
      }

      // add the additional server infos for the plugin
      for (int i = 0; i < serverInfos.length; i++) {
         final ServerInfo serverInfo = serverInfos[i];
         final ExtensionServerInfo esi = new ExtensionServerInfo();
         if (serverInfo.url == null) {
            throw new IllegalArgumentException("No URL was specified for a server info.");
         }
         if (!isPluginUrlHttp(serverInfo.url) && !isPluginUrlHttps(serverInfo.url)) {
            throw new IllegalArgumentException(String.format(
                  "Invalid protocol specified for server info url %s." +
                        " Only http/https are supported.", serverInfo.url));
         }
         esi.setUrl(serverInfo.url);
         esi.setServerThumbprint(serverInfo.serverThumbprint);
         try {
            esi.setServerCertificate(readCertificate(serverInfo.serverCertificateFile));
         } catch (IOException | CertificateException e) {
            throw new IllegalArgumentException(String.format("Could not read the certificate file" +
                  " specified in $[%s].serverCertificateFile of the \"pluginServers\" parameter.", i), e);
         }
         esi.setType(
               serverInfo.type != null ? serverInfo.type : DEFAULT_SERVER_TYPE);
         final Description description = new Description();
         description.setLabel(serverInfo.label != null ? serverInfo.label : "");
         description.setSummary(serverInfo.summary != null ? serverInfo.summary : "");
         esi.setDescription(description);
         esi.setCompany(
               serverInfo.company != null ? serverInfo.company : "");
         esi.getAdminEmail()
               .add(serverInfo.adminEmail != null ? serverInfo.adminEmail : ADMIN_EMAIL);
         servers.add(esi);
      }
   }

   /**
    * Updates last modification time of a given extension.
    *
    * @param extension - the current extension's instance
    * @throws DatatypeConfigurationException
    */
   public void updatelastHeartbeatTime(Extension extension)
         throws DatatypeConfigurationException {
      GregorianCalendar cal = new GregorianCalendar(
            TimeZone.getTimeZone("GMT"));
      DatatypeFactory dtFactory = DatatypeFactory.newInstance();
      XMLGregorianCalendar xmlCalendar = dtFactory.newXMLGregorianCalendar(cal);
      extension.setLastHeartbeatTime(xmlCalendar);
   }

   // Private helper methods

   /**
    * Checks if the provided pluginUrl parameter is HTTPS.
    */
   private boolean isPluginUrlHttps(String pluginUrl) {
      return pluginUrl != null && pluginUrl.toLowerCase()
            .startsWith(HTTPS_PROTOCOL.toLowerCase());
   }

   /**
    * Checks if the provided pluginUrl parameter is HTTP.
    */
   private boolean isPluginUrlHttp(String pluginUrl) {
      return pluginUrl != null && pluginUrl.toLowerCase()
            .startsWith(HTTP_PROTOCOL.toLowerCase().concat("://"));
   }

   private static class ServerInfo {
      public String url;
      public String type;
      public String serverThumbprint;
      public String serverCertificateFile;
      public String label;
      public String summary;
      public String company;
      public String adminEmail;
   }
}
