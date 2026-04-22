/*
 * ******************************************************************
 * Copyright (c) 2020-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.automatic.plugin.registration.services;

import com.vmware.automatic.plugin.registration.resources.CommandLineBuilder;
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
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.GregorianCalendar;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class PluginExtensionRegistryServiceTest {

   private PluginExtensionRegistryService extensionRegistryService;

   @BeforeClass
   private void init() {
      extensionRegistryService = new PluginExtensionRegistryService();
   }

   // updateTopLevelProperties
   @Test
   public void updateTopLevelProperties_whenExtensionHasNoKey_setsExtensionKey()
         throws Exception {
      // Pre-update extension
      final Extension extension = new Extension();
      // Command line arguments
      final CommandLine commandLine = new CommandLineBuilder().defaults()
            .build();
      extensionRegistryService.updateTopLevelProperties(extension, commandLine);
      // Asserts
      assertEquals(extension.getKey(), "plugin-key");
   }

   @Test
   public void updateTopLevelProperties_setsProvidedValues()
         throws Exception {
      // Pre-update extension
      final Extension extension = new Extension();
      final String key = "extension-key";
      extension.setKey(key);
      // Command line arguments
      final String version = "test-version";
      final String company = "test-company";

      final CommandLine commandLine = new CommandLineBuilder()
            .defaults()
            .version(version)
            .company(company)
            .build();
      extensionRegistryService.updateTopLevelProperties(extension, commandLine);
      // Asserts
      assertEquals(extension.getKey(), key);
      assertEquals(extension.getVersion(), version);
      assertEquals(extension.getCompany(), company);
   }

   // updateDescription
   @Test
   public void updateDescription_whenDescriptionIsNullAndNoDescriptionMembersProvided_setsDefaultValues()
         throws ParseException {
      // Pre-update extension
      final Extension extension = new Extension();
      // Post update description values
      final String label = "";
      final String summary = "";
      // Command line arguments
      final CommandLine commandLine = new CommandLineBuilder()
            .defaults()
            .build();
      extensionRegistryService.updateDescription(extension, commandLine);
      // Asserts
      assertEquals(extension.getDescription().getLabel(), label);
      assertEquals(extension.getDescription().getSummary(), summary);
   }

   @Test
   public void updateDescription_whenDescriptionIsNullAndDescriptionMembersProvided_setsProvidedValues()
         throws ParseException {
      // Pre-update extension
      final Extension extension = new Extension();
      // Post update description values
      final String label = "test label";
      final String summary = "test description";
      // Command line arguments
      final CommandLine commandLine = new CommandLineBuilder()
            .defaults()
            .label(label)
            .summary(summary)
            .build();
      extensionRegistryService.updateDescription(extension, commandLine);
      // Asserts
      assertEquals(extension.getDescription().getLabel(), label);
      assertEquals(extension.getDescription().getSummary(), summary);
   }

   @Test
   public void updateDescription_whenDescriptionIsNotNullAndNoDescriptionMembersProvided_keepsExistingValues()
         throws ParseException {
      // Pre-update description values
      final String label = "pre-update test label";
      final String summary = "pre-update test description";
      // Pre-update extension
      final Extension extension = new Extension();
      extension.setDescription(new Description());
      extension.getDescription().setLabel(label);
      extension.getDescription().setSummary(summary);
      // Command line arguments
      final CommandLine commandLine = new CommandLineBuilder()
            .defaults()
            .build();

      extensionRegistryService.updateDescription(extension, commandLine);
      // Asserts
      assertEquals(extension.getDescription().getLabel(), label);
      assertEquals(extension.getDescription().getSummary(), summary);
   }

   @Test
   public void updateDescription_whenDescriptionIsNotNullAndDescriptionMembersProvided_setsProvidedValues()
         throws ParseException {
      // Pre-update description values
      final String preUpdateLabel = "pre-update test label";
      final String preUpdateSummary = "pre-update test description";
      // Post update description values
      final String postUpdateLabel = "post-update test label";
      final String postUpdateSummary = "post-update test description";
      // Pre-update extension
      final Extension extension = new Extension();
      extension.setDescription(new Description());
      extension.getDescription().setLabel(preUpdateLabel);
      extension.getDescription().setSummary(preUpdateSummary);
      // Command line arguments
      final CommandLine commandLine = new CommandLineBuilder()
            .defaults()
            .label(postUpdateLabel)
            .summary(postUpdateSummary)
            .build();

      extensionRegistryService.updateDescription(extension, commandLine);
      // Asserts
      assertEquals(extension.getDescription().getLabel(), postUpdateLabel);
      assertEquals(extension.getDescription().getSummary(), postUpdateSummary);
   }

   // updateClientInfo
   @Test
   public void updateClientInfo_whenClientInfoIsEmptyAndNoClientInfoMembersProvided_setsDefaultValues()
         throws ParseException {
      // Pre-update values
      final Extension extension = new Extension();
      extension.setDescription(new Description());
      extension.getDescription().setSummary("test-summary");
      extension.getDescription().setLabel("test-description");
      // Command line arguments
      final CommandLine commandLine = new CommandLineBuilder()
            .defaults()
            .build();
      extensionRegistryService.updateClientInfo(extension, commandLine);
      // Asserts
      final ExtensionClientInfo clientInfo = extension.getClient().get(0);
      assertEquals(clientInfo.getCompany(), "");
      assertEquals(clientInfo.getType(), "vsphere-client-remote");
      assertEquals(clientInfo.getDescription().getSummary(), "test-summary");
      assertEquals(clientInfo.getDescription().getLabel(), "test-description");
      assertNull(clientInfo.getUrl());
      assertNull(clientInfo.getVersion());
   }

   @Test
   public void updateClientInfo_whenClientInfoIsEmptyAndClientInfoMembersProvided_setsProvidedValues()
         throws ParseException {
      // Pre-update values
      final Extension extension = new Extension();
      final Description description = new Description();
      extension.setDescription(description);
      description.setSummary("test-summary");
      description.setLabel("test-description");

      // Post-update values
      final String postUpdateVersion = "updated-test-version";
      final String postUpdatePluginUrl = "updated-test-plugin-url";

      // Command line arguments
      final CommandLine commandLine = new CommandLineBuilder()
            .defaults()
            .version(postUpdateVersion)
            .pluginUrl(postUpdatePluginUrl)
            .build();
      extensionRegistryService.updateClientInfo(extension, commandLine);

      // Asserts
      final ExtensionClientInfo clientInfo = extension.getClient().get(0);
      assertEquals(clientInfo.getCompany(), "");
      assertEquals(clientInfo.getType(), "vsphere-client-remote");
      assertEquals(clientInfo.getDescription().getSummary(),
            description.getSummary());
      assertEquals(clientInfo.getDescription().getLabel(),
            description.getLabel());
      assertEquals(clientInfo.getUrl(), postUpdatePluginUrl);
      assertEquals(clientInfo.getVersion(), postUpdateVersion);
   }

   @Test
   public void updateClientInfo_whenClientInfoIsNotEmptyAndSomeClientInfoMembersProvided_setsProvidedValues()
         throws ParseException {
      // Pre-update values
      final Extension extension = new Extension();
      // Description
      extension.setDescription(new Description());
      extension.getDescription().setSummary("test-summary");
      extension.getDescription().setLabel("test-description");
      // ClientInfo
      final ExtensionClientInfo extensionClientInfo = new ExtensionClientInfo();
      extensionClientInfo.setCompany("test-company");
      extensionClientInfo.setDescription(extension.getDescription());
      extensionClientInfo.setType("vsphere-client-remote");
      extensionClientInfo.setUrl("test-plugin-url");
      extensionClientInfo.setVersion("test-version");
      extension.getClient().add(extensionClientInfo);

      // Post-update values
      final String postUpdateVersion = "updated-test-version";
      final String postUpdatePluginUrl = "updated-test-plugin-url";
      final String postUpdateCompany = "updated-test-company";

      // Command line arguments
      CommandLine commandLine = new CommandLineBuilder()
            .defaults()
            .version(postUpdateVersion)
            .pluginUrl(postUpdatePluginUrl)
            .company(postUpdateCompany)
            .build();
      extensionRegistryService.updateClientInfo(extension, commandLine);

      // Asserts
      final ExtensionClientInfo clientInfo = extension.getClient().get(0);
      assertEquals(clientInfo.getType(), "vsphere-client-remote");
      assertEquals(clientInfo.getDescription().getSummary(), "test-summary");
      assertEquals(clientInfo.getDescription().getLabel(), "test-description");
      assertEquals(clientInfo.getCompany(), postUpdateCompany);
      assertEquals(clientInfo.getUrl(), postUpdatePluginUrl);
      assertEquals(clientInfo.getVersion(), postUpdateVersion);
   }

   @Test
   public void updateClientInfo_whenClientInfoIsNotEmptyAndNoClientInfoMembersProvided_keepsExistingValues()
         throws ParseException {
      // Pre-update values
      final Extension extension = new Extension();
      // Description
      extension.setDescription(new Description());
      extension.getDescription().setSummary("test-summary");
      extension.getDescription().setLabel("test-description");
      // ClientInfo
      final String preUpdateCompany = "test-company";
      final String preUpdatePluginUrl = "test-plugin-url";
      final String preUpdateVersion = "test-version";
      final ExtensionClientInfo extensionClientInfo = new ExtensionClientInfo();
      extensionClientInfo.setDescription(extension.getDescription());
      extensionClientInfo.setType("vsphere-client-remote");
      extensionClientInfo.setCompany("test-company");
      extensionClientInfo.setUrl("test-plugin-url");
      extensionClientInfo.setVersion("test-version");
      extension.getClient().add(extensionClientInfo);

      // Command line arguments
      CommandLine commandLine = new CommandLineBuilder()
            .defaults()
            .build();
      extensionRegistryService.updateClientInfo(extension, commandLine);

      // Asserts
      final ExtensionClientInfo clientInfo = extension.getClient().get(0);
      assertEquals(clientInfo.getType(), "vsphere-client-remote");
      assertEquals(clientInfo.getDescription().getSummary(), "test-summary");
      assertEquals(clientInfo.getDescription().getLabel(), "test-description");
      assertEquals(clientInfo.getCompany(), preUpdateCompany);
      assertEquals(clientInfo.getUrl(), preUpdatePluginUrl);
      assertEquals(clientInfo.getVersion(), preUpdateVersion);
   }

   // update resourceInfo
   @Test
   public void updateTaskList()
         throws ParseException, FileNotFoundException {
      // Pre-update values
      final Extension extension = new Extension();

      final File taskListJson = saveResourceToTempFile("taskList.json");
      final CommandLine commadLine = new CommandLineBuilder()
            .taskList(taskListJson.getAbsolutePath())
            .defaults()
            .build();

      extensionRegistryService.updateTaskList(extension, commadLine);

      // Asserts
      assertEquals(extension.getTaskList().size(), 3);
      final ExtensionTaskTypeInfo taskInfo1 = extension.getTaskList().get(0);
      assertEquals(taskInfo1.getTaskID(),"com.vmware.sample.remote.Task1");
      final ExtensionTaskTypeInfo taskInfo2 = extension.getTaskList().get(1);
      assertEquals(taskInfo2.getTaskID(),"com.vmware.sample.remote.Task2");
      final ExtensionTaskTypeInfo taskInfo3 = extension.getTaskList().get(2);
      assertEquals(taskInfo3.getTaskID(),"com.vmware.sample.remote.Task3");
   }

   // update resourceInfo
   @Test
   public void updateFaultList()
         throws ParseException, FileNotFoundException {
      // Pre-update values
      final Extension extension = new Extension();

      final File faultListJson = saveResourceToTempFile("faultList.json");
      final CommandLine commadLine = new CommandLineBuilder()
            .faultList(faultListJson.getAbsolutePath())
            .defaults()
            .build();

      extensionRegistryService.updateFaultList(extension, commadLine);

      // Asserts
      assertEquals(extension.getFaultList().size(), 3);
      final ExtensionFaultTypeInfo faultInfo1 = extension.getFaultList().get(0);
      assertEquals(faultInfo1.getFaultID(),"com.vmware.sample.X1");
      final ExtensionFaultTypeInfo faultnfo2 = extension.getFaultList().get(1);
      assertEquals(faultnfo2.getFaultID(),"com.vmware.sample.X2");
      final ExtensionFaultTypeInfo faultInfo3 = extension.getFaultList().get(2);
      assertEquals(faultInfo3.getFaultID(),"com.vmware.sample.X3");
   }

   // update resourceInfo
   @Test
   public void updatePrivilegeList()
         throws ParseException, FileNotFoundException {
      // Pre-update values
      final Extension extension = new Extension();

      final File privilegeListJson = saveResourceToTempFile("privilegeList.json");
      final CommandLine commadLine = new CommandLineBuilder()
            .privilegeList(privilegeListJson.getAbsolutePath())
            .defaults()
            .build();

      extensionRegistryService.updatePrivilegeList(extension, commadLine);

      // Asserts
      assertEquals(extension.getPrivilegeList().size(), 4);
      final ExtensionPrivilegeInfo privilege1 = extension.getPrivilegeList().get(0);
      assertEquals(privilege1.getPrivGroupName(),"GroupId-1");
      assertEquals(privilege1.getPrivID(),"GroupId-1.Privilege-1");
      final ExtensionPrivilegeInfo privilege2 = extension.getPrivilegeList().get(1);
      assertEquals(privilege2.getPrivGroupName(),"GroupId-1");
      assertEquals(privilege2.getPrivID(),"GroupId-1.Privilege-2");
      final ExtensionPrivilegeInfo privilege3 = extension.getPrivilegeList().get(2);
      assertEquals(privilege3.getPrivGroupName(),"GroupId-2");
      assertEquals(privilege3.getPrivID(),"GroupId-2.Privilege-1");
      final ExtensionPrivilegeInfo privilege4 = extension.getPrivilegeList().get(3);
      assertEquals(privilege4.getPrivGroupName(),"GroupId-2");
      assertEquals(privilege4.getPrivID(),"GroupId-2.Privilege-2");
   }

   @Test
   public void updateEventList()
         throws ParseException, FileNotFoundException {
      // Pre-update values
      final Extension extension = new Extension();

      final File eventListJson = saveResourceToTempFile("eventList.json");
      final CommandLine commandLine = new CommandLineBuilder()
            .eventList(eventListJson.getAbsolutePath())
            .defaults()
            .build();

      extensionRegistryService.updateEventList(extension, commandLine);

      // Asserts
      assertEquals(extension.getEventList().size(), 4);
      final ExtensionEventTypeInfo eventTypeInfo1 = extension.getEventList().get(0);
      assertEquals(eventTypeInfo1.getEventID(),"com.vmware.event1");
      assertEquals(eventTypeInfo1.getEventTypeSchema(),"<EventType><eventTypeID>com.vmware.event1.type</eventTypeID><description>Event1 description.</description><arguments><argument><name>arg1</name><type>string</type></argument><argument><name>arg2</name><type>bool</type></argument></arguments></EventType>");
      final ExtensionEventTypeInfo eventTypeInfo2 = extension.getEventList().get(1);
      assertEquals(eventTypeInfo2.getEventID(),"com.vmware.event2");
      assertEquals(eventTypeInfo2.getEventTypeSchema(),"<EventType><eventTypeID>com.vmware.event2.type</eventTypeID><description>Event2 description.</description><arguments><argument><name>arg3</name><type>int</type></argument><argument><name>arg4</name><type>moid</type></argument></arguments></EventType>");
      final ExtensionEventTypeInfo eventTypeInfo3 = extension.getEventList().get(2);
      assertEquals(eventTypeInfo3.getEventID(),"com.vmware.event3");
      assertEquals(eventTypeInfo3.getEventTypeSchema(),"<EventType><eventTypeID>com.vmware.event3.type</eventTypeID><description>Event3 description.</description><arguments><argument><name>arg5</name><type>long</type></argument><argument><name>arg6</name><type>float</type></argument></arguments></EventType>");
      final ExtensionEventTypeInfo eventTypeInfo4 = extension.getEventList().get(3);
      assertEquals(eventTypeInfo4.getEventID(),"com.vmware.event4");
      assertNull(eventTypeInfo4.getEventTypeSchema());
   }

   // update resourceInfo
   @Test
   public void updateResourceList()
         throws ParseException, FileNotFoundException {
      // Pre-update values
      final Extension extension = new Extension();

      final File resourceListJson = saveResourceToTempFile("resourceList.json");
      final CommandLine commadLine = new CommandLineBuilder()
            .resourceList(resourceListJson.getAbsolutePath())
            .defaults()
            .build();

      extensionRegistryService.updateResourceInfo(extension, commadLine);

      // Asserts
      assertEquals(extension.getResourceList().size(), 3);
      final ExtensionResourceInfo resource1 = extension.getResourceList().get(0);
      assertEquals(resource1.getLocale(),"en");
      final List<KeyValue> data1 = resource1.getData();
      assertEquals(data1.get(0).getKey(), "com.vmware.sample.remote.X1");
      assertEquals(data1.get(0).getValue(), "Task 1");
      assertEquals(data1.get(1).getKey(), "com.vmware.sample.remote.faults.X.summary");
      assertEquals(data1.get(1).getValue(), "Fault Summary.");
      assertEquals(data1.get(2).getKey(), "privilege.GroupId-1.Privilege-1.label");
      assertEquals(data1.get(2).getValue(), "Label 1");

      final ExtensionResourceInfo resource2 = extension.getResourceList().get(1);
      assertEquals(resource2.getLocale(),"de");
      final List<KeyValue> data2 = resource2.getData();
      assertEquals(data2.get(0).getKey(), "com.vmware.sample.remote.X1");
      assertEquals(data2.get(0).getValue(), "Task 1-DE");
      assertEquals(data2.get(1).getKey(), "com.vmware.sample.remote.faults.X.summary");
      assertEquals(data2.get(1).getValue(), "Fault Summary-DE.");
      assertEquals(data2.get(2).getKey(), "privilege.GroupId-1.Privilege-1.label");
      assertEquals(data2.get(2).getValue(), "Label 1-DE");

      final ExtensionResourceInfo resource3 = extension.getResourceList().get(2);
      assertEquals(resource3.getLocale(),"fr");
      final List<KeyValue> data3 = resource3.getData();
      assertEquals(data3.get(0).getKey(), "com.vmware.sample.remote.X1");
      assertEquals(data3.get(0).getValue(), "Task 1-FR");
      assertEquals(data3.get(1).getKey(), "com.vmware.sample.remote.faults.X.summary");
      assertEquals(data3.get(1).getValue(), "Fault Summary-FR.");
      assertEquals(data3.get(2).getKey(), "privilege.GroupId-1.Privilege-1.label");
      assertEquals(data3.get(2).getValue(), "Label 1-FR");
   }

   // updateServerInfo
   @Test
   public void updateServerInfo_whenServerInfoIsEmptyAndProvidedServerInfoIsHTTP_addsManifestServerInfo()
         throws ParseException {
      final PrintStream originalSystemOut = System.out;
      try {
         final Extension extension = new Extension();
         // Set system's out in order to test INFO message
         final ByteArrayOutputStream printedContent = new ByteArrayOutputStream();
         System.setOut(new PrintStream(printedContent));
         // Expected INFO message
         final String expectedInfoMsg =
               "INFO: Not using https for your plugin URL is OK for testing but not recommended for production."
                     + "\nUsers will have to include the flag allowHttp=true in their vSphere Client webclient.properties otherwise the http URL will be ignored"
                     + System.lineSeparator();
         final String postUpdateHttpUrl = "http://test-plugin-url.com";
         CommandLine commandLine = new CommandLineBuilder()
               .defaults()
               .pluginUrl(postUpdateHttpUrl)
               .build();
         extensionRegistryService.updateServerInfo(extension, commandLine);
         assertEquals(extension.getServer().size(), 1);
         assertEquals(extension.getServer().get(0).getUrl(), postUpdateHttpUrl);
         assertEquals(printedContent.toString(), expectedInfoMsg);
         // Restore original system's out
         System.setOut(System.out);
      } finally {
         System.setOut(originalSystemOut);
      }
   }

   @Test
   public void updateServerInfo_pluginServersPreservedOnUpdate() throws ParseException {
      final Extension extension = new Extension();
      final ExtensionServerInfo previousServerInfo = new ExtensionServerInfo();
      final String prevServerInfoUrl = "https://test-si-url.com";
      final String updatedUrl = "http://test-plugin-url.com";
      previousServerInfo.setUrl(prevServerInfoUrl);
      extension.getServer().add(buildExtServerInfo(updatedUrl));
      extension.getServer().add(buildExtServerInfo(prevServerInfoUrl));

      CommandLine commadLine = new CommandLineBuilder()
            .defaults()
            .pluginUrl(updatedUrl)
            .build();
      extensionRegistryService.updateServerInfo(extension, commadLine);
      assertEquals(extension.getServer().size(), 2);
      assertEquals(extension.getServer().get(0).getUrl(), updatedUrl);
      assertEquals(extension.getServer().get(1).getUrl(), prevServerInfoUrl);
   }

   @Test
   public void updateServerInfo_whenServerInfoIsHTTPSAndNoServerInfoMembersProvided_doesNotSetAnything()
         throws ParseException {
      final Extension extension = new Extension();
      final ExtensionServerInfo serverInfo = new ExtensionServerInfo();
      serverInfo.setUrl("https://existing-test-url");
      serverInfo.setType("MANIFEST_SERVER");
      extension.getServer().add(serverInfo);

      final CommandLine commadLine = new CommandLineBuilder()
            .defaults()
            .build();
      extensionRegistryService.updateServerInfo(extension, commadLine);

      assertEquals(extension.getServer().size(), 1);
   }

   @Test
   public void updateServerInfo_whenServerInfoIsEmptyAndProvidedServerInfoIsHTTPS_setsCorrectValues()
         throws ParseException, IOException {
      Extension extension = new Extension();

      // Post-update values
      final String postUpdateHttpsUrl = "https://test-url.com";
      final String postUpdateServerThumbprint = "test:server:thumbprint";
      final File postUpdateCertificateFile = saveResourceToTempFile("certificate-valid.cer");
      final Path postUpdateCertificateFilePath = postUpdateCertificateFile.toPath();
      final String postUpdateCertificateFileContents = new String(Files.readAllBytes(postUpdateCertificateFilePath));

      CommandLine commandLine = new CommandLineBuilder()
            .defaults()
            .pluginUrl(postUpdateHttpsUrl)
            .serverThumbprint(postUpdateServerThumbprint)
            .serverCertificateFile(postUpdateCertificateFilePath.toString())
            .build();
      extensionRegistryService.updateServerInfo(extension, commandLine);

      // Asserts
      assertEquals(extension.getServer().size(), 1);
      final ExtensionServerInfo serverInfo = extension.getServer().get(0);
      assertEquals(serverInfo.getAdminEmail().size(), 1);
      assertEquals(serverInfo.getAdminEmail().get(0), "noreply@vmware.com");
      assertEquals(serverInfo.getType(), "MANIFEST_SERVER");
      assertEquals(serverInfo.getCompany(), "");
      assertNull(serverInfo.getDescription());
      assertEquals(serverInfo.getUrl(), postUpdateHttpsUrl);
      assertEquals(serverInfo.getServerThumbprint(), postUpdateServerThumbprint);
      assertEquals(serverInfo.getServerCertificate(), postUpdateCertificateFileContents);
   }

   @Test
   public void updateServerInfo_whenServerInfoIsEmptyAndProvidedServerInfoIsHTTPSAndNoServerThumbprintAndNoServerCertificateProvided_createsServerInfoWithNoThumbprint()
         throws ParseException {
      final Extension extension = new Extension();

      final CommandLine commandLine = new CommandLineBuilder()
            .defaults()
            .pluginUrl("https://test-plugin-url")
            .build();
      extensionRegistryService.updateServerInfo(extension, commandLine);

      // Asserts
      assertNotNull(extension.getServer());
      assertEquals(extension.getServer().size(), 1);
      final ExtensionServerInfo serverInfo = extension.getServer().get(0);
      assertNull(serverInfo.getServerThumbprint());
      assertNull(serverInfo.getServerCertificate());
   }

   @Test
   public void updateServerInfo_whenServerInfoIsNotEmptyAndProvidedServerInfoIsHTTPSButNoServerThumbprintAndNoServerCertificateProvided_updatesProvidedValuesOnly()
         throws ParseException {
      // Pre-update values
      final Extension extension = new Extension();
      final ExtensionServerInfo preUpdateServerInfo = new ExtensionServerInfo();
      final String preUpdateServerThumbprint = "pre-update-thumbprint";
      final String preUpdateServerCertificate = "pre-update-certificate";
      final String preUpdateAdminEmail = "admin@yahoo.com";
      final String preUpdatePluginUrl = "https://pre-update-url";
      final String preUpdateLabel = "pre-update-label";
      final String preUpdateSummary = "pre-update-summary";
      final String preUpdateType = "MANIFEST_SERVER";
      preUpdateServerInfo.setCompany("");
      preUpdateServerInfo.setType(preUpdateType);
      preUpdateServerInfo.getAdminEmail().add(preUpdateAdminEmail);
      preUpdateServerInfo.setUrl(preUpdatePluginUrl);
      preUpdateServerInfo.setServerThumbprint(preUpdateServerThumbprint);
      preUpdateServerInfo.setServerCertificate(preUpdateServerCertificate);
      extension.setDescription(new Description());
      preUpdateServerInfo.setDescription(extension.getDescription());
      preUpdateServerInfo.getDescription().setLabel(preUpdateLabel);
      preUpdateServerInfo.getDescription().setSummary(preUpdateSummary);
      extension.getServer().add(preUpdateServerInfo);

      // Post-update values
      final String postUpdateCompany = "post-update-company";
      final CommandLine commandLine = new CommandLineBuilder()
            .defaults()
            .company(postUpdateCompany)
            .build();
      extensionRegistryService.updateServerInfo(extension, commandLine);

      // Asserts
      assertEquals(extension.getServer().size(), 1);
      final ExtensionServerInfo postUpdateServerInfo = extension.getServer()
            .get(0);
      assertEquals(postUpdateServerInfo.getServerThumbprint(),
            preUpdateServerThumbprint);
      assertEquals(postUpdateServerInfo.getServerCertificate(),
            preUpdateServerCertificate);
      assertEquals(postUpdateServerInfo.getAdminEmail().get(0),
            preUpdateAdminEmail);
      assertEquals(postUpdateServerInfo.getUrl(), preUpdatePluginUrl);
      assertEquals(postUpdateServerInfo.getType(), preUpdateType);
      assertEquals(postUpdateServerInfo.getDescription().getLabel(),
            preUpdateLabel);
      assertEquals(postUpdateServerInfo.getDescription().getSummary(),
            preUpdateSummary);
      assertEquals(postUpdateServerInfo.getCompany(), postUpdateCompany);
   }

   @Test
   public void updateServerInfo_whenServerInfoIsHTTPSAndProvidedPluginUrlIsHTTP_adjustsServerInfo()
         throws ParseException {
      final Extension extension = new Extension();
      final ExtensionServerInfo serverInfo = new ExtensionServerInfo();
      serverInfo.setUrl("https://existing-test-url");
      serverInfo.setType("HTTPS");
      extension.getServer().add(serverInfo);

      final CommandLine commadLine = new CommandLineBuilder()
            .defaults()
            .pluginUrl("http://new-plugin-url")
            .company("test-company")
            .build();
      extensionRegistryService.updateServerInfo(extension, commadLine);

      assertEquals(extension.getServer().size(), 1);
      assertEquals(extension.getServer().get(0).getUrl(), "http://new-plugin-url");
   }

   @Test
   public void updateServerInfo_whenServerInfoIsNotPresent_addAdditionalServerInfos()
         throws ParseException, IOException {
      final File serverCertificateFile = saveResourceToTempFile("certificate-valid.cer");
      final Path serverCertificateFilePath = serverCertificateFile.toPath();
      final String serverCertificateFileContents = new String(Files.readAllBytes(serverCertificateFilePath));

      final Extension extension = new Extension();

      final String pluginServersJson =
            "[{" +
            "\"url\" : \"https://10.30.199.200:8443/vum-instance-1/vum-ui/plugin.json\"," +
            "\"type\" : \"MY_PLUGIN_SERVER\"," +
            "\"serverThumbprint\" : \"D1:07:9A:AA:EA:16:C3:7C:EA:02:8F:96:7C:8B:EE:F0:45:97:13:55\"," +
            String.format("\"serverCertificateFile\" : \"%s\",", serverCertificateFilePath) +
            "\"company\" : \"myCompany\"," +
            "\"label\" : \"myLabel\"," +
            "\"summary\" : \"mySummary\"," +
            "\"adminEmail\" : \"myMail\"" +
            "}," +
            "{\"url\" : \"https://10.30.199.200:8443/vum-instance-1/vum-ui/plugin.json\"" +
            "}]";
      final CommandLine commadLine = new CommandLineBuilder()
            .defaults()
            .pluginUrl("https://new-plugin-url")
            .pluginServers(pluginServersJson)
            .build();
      extensionRegistryService.updateServerInfo(extension, commadLine);
      assertEquals(extension.getServer().size(), 3);
      assertEquals(extension.getServer().get(0).getUrl(), "https://new-plugin-url");
      assertEquals(extension.getServer().get(0).getType(), "MANIFEST_SERVER");

      // verify first server info fields
      assertEquals(extension.getServer().get(1).getUrl(),
            "https://10.30.199.200:8443/vum-instance-1/vum-ui/plugin.json");
      assertEquals(extension.getServer().get(1).getType(), "MY_PLUGIN_SERVER");
      assertEquals(extension.getServer().get(1).getServerThumbprint(),
            "D1:07:9A:AA:EA:16:C3:7C:EA:02:8F:96:7C:8B:EE:F0:45:97:13:55");
      assertEquals(extension.getServer().get(1).getServerCertificate(),
            serverCertificateFileContents);
      assertEquals(extension.getServer().get(1).getCompany(), "myCompany");
      assertEquals(extension.getServer().get(1).getDescription().getLabel(), "myLabel");
      assertEquals(extension.getServer().get(1).getDescription().getSummary(),
            "mySummary");
      assertEquals(extension.getServer().get(1).getAdminEmail().get(0), "myMail");

      // verify second server info fields
      assertEquals(extension.getServer().get(2).getUrl(),
            "https://10.30.199.200:8443/vum-instance-1/vum-ui/plugin.json");
      assertEquals(extension.getServer().get(2).getType(), "PLUGIN_SERVER");
      assertNull(extension.getServer().get(2).getServerThumbprint());
      assertNull(extension.getServer().get(2).getServerCertificate());
      assertEquals(extension.getServer().get(2).getCompany(), "");
      assertEquals(extension.getServer().get(2).getDescription().getLabel(), "");
      assertEquals(extension.getServer().get(2).getDescription().getSummary(), "");
      assertEquals(extension.getServer().get(2).getAdminEmail().get(0),
            "noreply@vmware.com");
   }


   @Test
   public void updateServerInfo_whenServerInfoIsPresent_addAdditionalServerInfosOverwritePreviousServerInfos()
         throws ParseException, IOException {
      final File serverCertificateFile = saveResourceToTempFile("certificate-valid.cer");
      final Path serverCertificateFilePath = serverCertificateFile.toPath();
      final String serverCertificateFileContents = new String(Files.readAllBytes(serverCertificateFilePath));

      final Extension extension = new Extension();
      final ExtensionServerInfo serverInfo = new ExtensionServerInfo();
      serverInfo.setUrl("https://existing-test-url");
      serverInfo.setType("HTTPS");
      extension.getServer().add(serverInfo);
      final String pluginServersJson =
            "[{" +
            "\"url\" : \"https://10.30.199.200:8443/vum-instance-1/vum-ui/plugin.json\"," +
            "\"type\" : \"MY_PLUGIN_SERVER\"," +
            "\"serverThumbprint\" : \"D1:07:9A:AA:EA:16:C3:7C:EA:02:8F:96:7C:8B:EE:F0:45:97:13:55\"," +
            String.format("\"serverCertificateFile\" : \"%s\",", serverCertificateFilePath) +
            "\"company\" : \"myCompany\"," +
            "\"label\" : \"myLabel\"," +
            "\"summary\" : \"mySummary\"," +
            "\"adminEmail\" : \"myMail\"" +
            "}," +
            "{\"url\" : \"https://10.30.199.200:8443/vum-instance-1/vum-ui/plugin.json\"" +
            "}]";
      final CommandLine commadLine = new CommandLineBuilder()
            .defaults()
            .pluginUrl("https://new-plugin-url")
            .pluginServers(pluginServersJson)
            .build();
      extensionRegistryService.updateServerInfo(extension, commadLine);
      assertEquals(extension.getServer().size(), 3);
      assertEquals(extension.getServer().get(0).getUrl(), "https://new-plugin-url");
      assertEquals(extension.getServer().get(0).getType(), "MANIFEST_SERVER");

      // verify first server info fields
      assertEquals(extension.getServer().get(1).getUrl(),
            "https://10.30.199.200:8443/vum-instance-1/vum-ui/plugin.json");
      assertEquals(extension.getServer().get(1).getType(), "MY_PLUGIN_SERVER");
      assertEquals(extension.getServer().get(1).getServerThumbprint(),
            "D1:07:9A:AA:EA:16:C3:7C:EA:02:8F:96:7C:8B:EE:F0:45:97:13:55");
      assertEquals(extension.getServer().get(1).getServerCertificate(),
            serverCertificateFileContents);
      assertEquals(extension.getServer().get(1).getCompany(), "myCompany");
      assertEquals(extension.getServer().get(1).getDescription().getLabel(), "myLabel");
      assertEquals(extension.getServer().get(1).getDescription().getSummary(),
            "mySummary");
      assertEquals(extension.getServer().get(1).getAdminEmail().get(0), "myMail");

      // verify second server info fields
      assertEquals(extension.getServer().get(2).getUrl(),
            "https://10.30.199.200:8443/vum-instance-1/vum-ui/plugin.json");
      assertEquals(extension.getServer().get(2).getType(), "PLUGIN_SERVER");
      assertNull(extension.getServer().get(2).getServerThumbprint());
      assertNull(extension.getServer().get(2).getServerCertificate());
      assertEquals(extension.getServer().get(2).getCompany(), "");
      assertEquals(extension.getServer().get(2).getDescription().getLabel(), "");
      assertEquals(extension.getServer().get(2).getDescription().getSummary(), "");
      assertEquals(extension.getServer().get(2).getAdminEmail().get(0),
            "noreply@vmware.com");
   }

   @Test
   public void updateCalendarInfo_setsLastModificationTime()
         throws DatatypeConfigurationException {
      // Create calendar mocks
      MockedStatic<DatatypeFactory> datatypeFactoryMockedStatic = Mockito.mockStatic(
            DatatypeFactory.class);
      DatatypeFactory dtFactoryMock = Mockito.mock(DatatypeFactory.class);
      XMLGregorianCalendar xmlCalendarMock = Mockito
            .mock(XMLGregorianCalendar.class);
      // Expect mocks
      datatypeFactoryMockedStatic.when( () -> DatatypeFactory.newInstance()).thenReturn(dtFactoryMock);
      Mockito.when(dtFactoryMock
            .newXMLGregorianCalendar(Mockito.any(GregorianCalendar.class)))
            .thenReturn(xmlCalendarMock);

      // Execute method
      final Extension extension = new Extension();
      extensionRegistryService.updatelastHeartbeatTime(extension);
      datatypeFactoryMockedStatic.close();

      // Assert lastHeartbeatTime
      assertEquals(extension.getLastHeartbeatTime(), xmlCalendarMock);
   }

   private static ExtensionServerInfo buildExtServerInfo(final String url) {
      final ExtensionServerInfo extensionServerInfo = new ExtensionServerInfo();
      extensionServerInfo.setUrl(url);
      return extensionServerInfo;
   }

   private File saveResourceToTempFile(final String resourcePath) {
      try {
         final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
         final Path targetFilePath = Files.createTempFile(null, null);
         final File targetFile = targetFilePath.toFile();
         final OutputStream targetFileOutputStream = Files.newOutputStream(targetFile.toPath());
         IOUtils.copy(inputStream, targetFileOutputStream);
         return targetFile;
      } catch (IOException e) {
         fail("Could not save resource file " + resourcePath + " to temp dir");
         return null;
      }
   }
}
