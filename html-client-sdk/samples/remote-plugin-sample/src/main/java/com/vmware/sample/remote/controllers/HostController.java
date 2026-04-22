/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

package com.vmware.sample.remote.controllers;

import java.util.List;
import java.util.stream.Collectors;

import com.vmware.sample.remote.model.Chassis;
import com.vmware.sample.remote.model.Host;
import com.vmware.sample.remote.services.ChassisService;
import com.vmware.sample.remote.services.HostService;
import org.apache.commons.lang3.Validate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * A controller which returns information about vSphere host objects.
 */
@RestController
@RequestMapping("/rest")
public class HostController {

   private final HostService vcenterInfoService;
   private final ChassisService chassisService;

   public HostController(final HostService vcenterInfoService,
         final ChassisService chassisService) {
      this.vcenterInfoService = vcenterInfoService;
      this.chassisService = chassisService;
   }

   /**
    * Retrieves all host objects connected to the vCenter Server.
    * Currently there is no relation between hosts/chassis, but everyone is
    * free to implement any kind of relation between them and filter them by a
    * provided parameter representing a chassis.
    * @return list of host objects.
    */
   @RequestMapping(value = "/hosts", method = RequestMethod.GET)
   public List<Host> retrieveConnectedHosts() {
      return vcenterInfoService.retrieveConnectedHosts();
   }

   /**
    * Retrieves all chassis related to the given Host object
    * @return list of related Chassis objects.
    */
   @RequestMapping(value = "/hosts/{hostId}/chassis", method = RequestMethod.GET)
   public List<Chassis> retrieveRelatedChassis(
         @PathVariable("hostId") final String hostId) {
      Validate.notNull(hostId);
      return chassisService.getRelatedChassis(hostId);
   }

   /**
    * Retrieves all connected host objects related to the given chassis.
    * Removes hosts from the relation that are no longer connected or
    * available to the vCenter Server.
    *
    * @return list of host objects filtered by the provided chassisId.
    */
   @RequestMapping(value = "/chassis/{chassisId}/hosts", method = RequestMethod.GET)
   public List<Host> retrieveConnectedHosts(
         @PathVariable("chassisId") final String chassisId) {
      final List<Host> hostsList = vcenterInfoService
            .retrieveConnectedHosts(chassisId);

      chassisService.setRelatedHosts(chassisId,
            hostsList.stream().map(host -> host.id)
                  .collect(Collectors.toList()));

      return hostsList;
   }

   @RequestMapping(value = "/hosts", method = RequestMethod.PUT)
   public void edit(@RequestBody final Host host) {
      chassisService.updateHostRelation(host);
   }

}
