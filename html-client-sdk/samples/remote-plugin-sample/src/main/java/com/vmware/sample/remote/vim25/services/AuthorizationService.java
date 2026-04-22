/*
 * ******************************************************************
 * Copyright (c) 2020-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote.vim25.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vmware.sample.remote.gateway.SessionService;
import com.vmware.sample.remote.gateway.VimSessionInfo;
import com.vmware.vim25.EntityPrivilege;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.PrivilegeAvailability;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

/**
 * Responsible for checking user privileges using vCenter Server's AuthorizationManager
 */
public class AuthorizationService {
   private static final String PROP_SERVICE_INSTANCE = "ServiceInstance";
   private static final Log logger = LogFactory.getLog(
         AuthorizationService.class);

   private final SessionService sessionService;
   private final ManagedObjectReference serviceInstanceRef = new ManagedObjectReference();

   public AuthorizationService(final SessionService sessionService) {
      this.sessionService = sessionService;
      serviceInstanceRef.setType(PROP_SERVICE_INSTANCE);
      serviceInstanceRef.setValue(PROP_SERVICE_INSTANCE);
   }

   public boolean hasPrivilege(final List<String> objectIds, final String privilegeId) {
      final VimSessionInfo vimSessionInfo = sessionService.getVimSessionInfo();
      final VimPortType vimPort = vimSessionInfo.getVimPort();

      final List<ManagedObjectReference> objectReferences = new ArrayList<>();
      for (final String objectId : objectIds) {
         // The complete objectId is with the following format:
         // urn:vmomi:VirtualMachine:vm-58:28a09968-ddd6-47e2-a28f-616048f32939
         // In order to construct the ManagedObjectReference, we need the type and id
         // for example, type: VirtualMachine, id: vm-58
         final ManagedObjectReference mor = new ManagedObjectReference();
         final String[] objectIdParts = objectId.split(":");
         mor.setType(objectIdParts[2]);
         mor.setValue(objectIdParts[3]);
         objectReferences.add(mor);
      }

      try {
         final ServiceContent serviceContent = vimPort.retrieveServiceContent(serviceInstanceRef);
         final ManagedObjectReference authorizationManagerMor = serviceContent.getAuthorizationManager();
         final List<EntityPrivilege> entityPrivileges =
               vimPort.hasPrivilegeOnEntities(authorizationManagerMor, objectReferences,
                     sessionService.getVimSessionInfo().getUserSession().getKey(), Arrays.asList(privilegeId));
         for (final EntityPrivilege entityPrivilege : entityPrivileges) {
            for (final PrivilegeAvailability privilegeAvailability : entityPrivilege.getPrivAvailability()) {
               if (!privilegeAvailability.isIsGranted()) {
                  return false;
               }
            }
         }
         return true;
      } catch (Exception ex) {
         logger.error("Unable to retrieve privilege information.", ex);
         return false;
      }
   }
}
