/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote.vim25.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vmware.sample.remote.configuration.Configuration;
import com.vmware.sample.remote.gateway.SessionService;
import com.vmware.sample.remote.model.Host;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.HostSystemConnectionState;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.VimPortType;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A bean talking to the vCenter Server using the provided vSphere web services
 * client library (vim25.jar).
 */
public class VimObjectService {
   private static final Log logger = LogFactory.getLog(
         VimObjectService.class);
   private static final String OBJECT_ID_FORMAT = "urn:vmomi:%s:%s:%s";
   private static final String PROP_SERVICE_INSTANCE = "ServiceInstance";
   private static final String PROP_NAME = "name";
   private static final String PROP_HOST = "HostSystem";
   private static final String PROP_HOST_SUMMARY = "summary.host";
   private static final String PROP_HOST_CONNECTION_STATE = "runtime.connectionState";
   private static final String PROP_NUM_CPU_CORES = "hardware.cpuInfo.numCpuCores";
   private static final String PROP_MEMORY_SIZE = "systemResources.config.memoryAllocation.limit";
   private static final String[] HOST_PROPERTIES = { PROP_NAME,
         PROP_HOST_SUMMARY, PROP_NUM_CPU_CORES, PROP_HOST_CONNECTION_STATE,
         PROP_MEMORY_SIZE };

   private final SessionService sessionService;
   private final Configuration configuration;

   public VimObjectService(final SessionService sessionService,
         final Configuration configuration) {
      this.sessionService = sessionService;
      this.configuration = configuration;
   }

   /**
    * Retrieves information about the vSphere Host Objects from a vCenter Server
    *
    * @return a list of Host objects
    */
   public List<Host> retrieveHosts() {
      List<Map<String, Object>> retrievedHosts = retrieveObjectProperties(
            configuration.getVcenterGuid(), PROP_HOST, HOST_PROPERTIES);

      return transformHostsPropertiesToObjects(retrievedHosts);
   }

   /**
    * For a given vcenterGuid(which specifies the vCenter Server), vSphere Object
    * and properties, retrieves the values of properties for the given
    * vSphere Object in the vCenter Server defined in the vSphereObjectProperties
    * <p>
    * Sets up PropertyCollector and ViewManager, Creates the PropertyFilterSpec,
    * retrieves data using the vimPort field and formats them for easier usage.
    *
    * @param vcenterGuid - specifies the GUID of the vCenter Server
    * @param vSphereObject - for which vSphere Object to retrieve the properties
    * @param vSphereObjectProperties - properties to be retrieved
    *
    * @return List of all vSphere Objects of the specified type.
    */
   public List<Map<String, Object>> retrieveObjectProperties(
         final String vcenterGuid, final String vSphereObject,
         final String[] vSphereObjectProperties) {
      Validate.notNull(vcenterGuid);
      Validate.notNull(vSphereObject);
      Validate.notNull(vSphereObjectProperties);
      ServiceContent serviceContent = null;

      final VimPortType vimPort = this.sessionService.getVimSessionInfo().getVimPort();

      try {
         serviceContent = getServiceContent(vimPort);
      } catch (RuntimeFaultFaultMsg runtimeFaultFaultMsg) {
         logger.warn(
               "Could not retrieve the ServiceContent using sessionCookie",
               runtimeFaultFaultMsg);
      }
      if (serviceContent == null) {
         return Collections.emptyList();
      }
      // Get references to the ViewManager and the PropertyCollector
      final ManagedObjectReference viewMgrRef = serviceContent.getViewManager();
      final ManagedObjectReference propColl = serviceContent
            .getPropertyCollector();

      // Create a container view for the vSphere Object.
      final List<String> vObjects = Collections.singletonList(vSphereObject);

      RetrieveResult props = null;
      try {
         ManagedObjectReference cViewRef = vimPort
               .createContainerView(viewMgrRef, serviceContent.getRootFolder(),
                     vObjects, true);

         props = retrieveProperties(vimPort, cViewRef, propColl, vSphereObject,
               vSphereObjectProperties);
      } catch (RuntimeFaultFaultMsg runtimeFaultFaultMsg) {
         logger.error("Could not create ContainerView for " + vSphereObject,
               runtimeFaultFaultMsg);
      } catch (InvalidPropertyFaultMsg invalidPropertyFaultMsg) {
         logger.error("Could not retrieveProperties for " + vSphereObject,
               invalidPropertyFaultMsg);
      }

      return formatRetrievedProperties(props);
   }

   /**
    * Formats the list of host properties to a real Host objects
    */
   private List<Host> transformHostsPropertiesToObjects(
         final List<Map<String, Object>> retrievedHosts) {
      final List<Host> hosts = new ArrayList<>();
      final String vCenterFqdn = configuration.getVcenterServerFqdn();
      final String vCenterGuid = configuration.getVcenterGuid();
      for (final Map<String, Object> retrievedHost : retrievedHosts) {
         final String objectId = getFormattedObjectId(retrievedHost,
               vCenterGuid);
         final String connectionState = getHostConnectionState(retrievedHost);

         final Object numCpuObject = retrievedHost.get(PROP_NUM_CPU_CORES);
         final Object memSizeObject = retrievedHost.get(PROP_MEMORY_SIZE);
         final String numCpus = (numCpuObject == null) ?
               "" :
               numCpuObject.toString();
         final String memSize = (memSizeObject == null) ?
               "" :
               memSizeObject.toString();

         final Host host = new Host(objectId,
               (String) retrievedHost.get(PROP_NAME), connectionState,
               vCenterFqdn, memSize, numCpus);

         hosts.add(host);
      }
      return hosts;
   }

   /**
    * Uses the host id and the serviceUrl to create the objectId for the current host
    */
   private String getFormattedObjectId(Map<String, Object> host, String serviceUrl) {
      String hostId = ((ManagedObjectReference) host.get(PROP_HOST_SUMMARY)).getValue();
      return String.format(OBJECT_ID_FORMAT, PROP_HOST, hostId, serviceUrl);
   }

   /**
    * Retrieves the connection state for the given host
    */
   private String getHostConnectionState(Map<String, Object> host) {
      HostSystemConnectionState conState =
            (HostSystemConnectionState) host.get(PROP_HOST_CONNECTION_STATE);
      return (conState == null) ? "" : conState.value();
   }

   /**
    * @return a ServiceContent instance
    *
    * @throws RuntimeFaultFaultMsg
    */
   private ServiceContent getServiceContent(VimPortType vimPort)
         throws RuntimeFaultFaultMsg {
      ManagedObjectReference serviceInstanceRef = createSvcInstanceRef();

      ServiceContent serviceContent = vimPort.retrieveServiceContent(serviceInstanceRef);

      return serviceContent;
   }

   /**
    * @return The Service Instance ManagedObjectReference
    */
   private ManagedObjectReference createSvcInstanceRef() {
      final ManagedObjectReference svcInstanceRef = new ManagedObjectReference();
      svcInstanceRef.setType(PROP_SERVICE_INSTANCE);
      svcInstanceRef.setValue(PROP_SERVICE_INSTANCE);

      return svcInstanceRef;
   }

   /**
    * @return Retrieved properties if it succeeds, null otherwise
    */
   private RetrieveResult retrieveProperties(VimPortType vimPort,
                                             ManagedObjectReference cViewRef,
                                             ManagedObjectReference propColl,
                                             String vSphereObject,
                                             String vSphereObjectProperties[])
         throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
      PropertyFilterSpec fSpec = createPropertyFilterSpec(cViewRef, vSphereObject,
            vSphereObjectProperties);

      List<PropertyFilterSpec> fSpecList = Collections.singletonList(fSpec);

      RetrieveOptions ro = new RetrieveOptions();
      RetrieveResult props = vimPort.retrievePropertiesEx(propColl, fSpecList, ro);

      return props;
   }

   /**
    * Creates PropertyFilterSpec which retrieves properties for the given vSphereObject
    * The properties are defined in the vSphereObjectProperties array
    *
    * @return the newly created PropertyFilterSpec
    */
   private PropertyFilterSpec createPropertyFilterSpec(
         ManagedObjectReference cViewRef, String vSphereObject,
         String[] vSphereObjectProperties) {
      // Creates an object specification to define the starting point for inventory navigation
      ObjectSpec oSpec = new ObjectSpec();
      oSpec.setObj(cViewRef);
      oSpec.setSkip(true);

      // Creates a traversal specification to identify the path for collection
      TraversalSpec tSpec = new TraversalSpec();
      tSpec.setName("traverseEntities");
      tSpec.setPath("view");
      tSpec.setSkip(false);
      tSpec.setType("ContainerView");

      // Adds the TraversalSpec to the ObjectSpec.selectSet array.
      oSpec.getSelectSet().add(tSpec);

      // Identify the properties to be retrieved.
      PropertySpec pSpec = new PropertySpec();
      pSpec.setType(vSphereObject);
      pSpec.getPathSet().addAll(Arrays.asList(vSphereObjectProperties));

      // Adds the object and property specifications to the property filter specification.
      PropertyFilterSpec fSpec = new PropertyFilterSpec();
      fSpec.getObjectSet().add(oSpec);
      fSpec.getPropSet().add(pSpec);

      return fSpec;
   }

   /**
    * Given RetrieveResult, converts the properties in a list of maps, where the maps
    * contain string keys(i.e. the property name) and Object values(i.e. the retrieved
    * properties)
    *
    * @param props containing the properties of the retrieved vSphere Object
    * @return The retrieved object in a more suitable format
    */
   private List<Map<String, Object>> formatRetrievedProperties(
         RetrieveResult props) {
      List<Map<String, Object>> objectsProperties = new ArrayList<>();

      if (props != null) {
         for (ObjectContent oc : props.getObjects()) {
            List<DynamicProperty> dps = oc.getPropSet();

            Map<String, Object> managedObject = new HashMap<>(dps.size());

            if (dps != null) {
               for (DynamicProperty dp : dps) {
                  managedObject.put(dp.getName(), dp.getVal());
               }
            }
            objectsProperties.add(managedObject);
         }
      }
      return objectsProperties;
   }
}
