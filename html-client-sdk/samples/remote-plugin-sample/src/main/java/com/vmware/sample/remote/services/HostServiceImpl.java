/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.vmware.sample.remote.model.Host;
import com.vmware.sample.remote.vim25.services.VimObjectService;
import org.springframework.util.CollectionUtils;

/**
 * Service used to retrieve information about HostSystem vSphere objects
 */
public class HostServiceImpl implements HostService {

   private final VimObjectService vimObjectService;
   private final ChassisService chassisService;

   public HostServiceImpl(final VimObjectService vimObjectService,
         final ChassisService chassisService) {
      this.vimObjectService = vimObjectService;
      this.chassisService = chassisService;
   }

   @Override
   public List<Host> retrieveConnectedHosts() {
      final List<Host> retrievedHosts = vimObjectService.retrieveHosts();
      return filterConnectedHosts(retrievedHosts);
   }

   @Override
   public List<Host> retrieveConnectedHosts(final String chassisId) {
      final List<String> relatedHostsIds = chassisService
            .getChassisById(chassisId).relatedHostsIds;
      if (CollectionUtils.isEmpty(relatedHostsIds)) {
         return Collections.emptyList();
      }
      final List<Host> connectedHosts = retrieveConnectedHosts();
      final List<Host> relatedHosts = connectedHosts.stream()
            .filter(host -> relatedHostsIds.contains(host.id))
            .collect(Collectors.toList());
      return relatedHosts;
   }

   /**
    * @return a list of all connected hosts
    */
   private List<Host> filterConnectedHosts(final List<Host> hosts) {
      final List<Host> result = new ArrayList<>();
      final String expectedState = "connected";
      for (final Host host : hosts) {
         if (expectedState.equals(host.state)) {
            // capitalize the first letter of the host state.
            host.state = Character
                  .toUpperCase(host.state.charAt(0)) + host.state.substring(1);
            result.add(host);
         }
      }

      return result;
   }
}
