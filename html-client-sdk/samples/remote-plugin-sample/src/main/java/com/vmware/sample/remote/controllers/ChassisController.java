/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

package com.vmware.sample.remote.controllers;

import java.util.List;

import com.vmware.sample.remote.model.Chassis;
import com.vmware.sample.remote.services.ChassisService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * A controller to serve requests to the endpoint "/rest".
 */
@RestController
@RequestMapping("/rest/chassis")
public class ChassisController {

   private final ChassisService chassisService;

   public ChassisController(final ChassisService chassisService) {
      this.chassisService = chassisService;
   }

   /**
    * Retrieves a chassis data by a given chassis id.
    *
    * @param objectId   id of the chassis object.
    * @return  the chassis object.
    */
   @RequestMapping(value = "/{objectId}", method = RequestMethod.GET)
   public Chassis getChassisById(
         @PathVariable("objectId") final String objectId) {
      return chassisService.getChassisById(objectId);
   }

   /**
    * Retrieves all chassis objects.
    *
    * @return list of chassis objects.
    */
   @RequestMapping(method = RequestMethod.GET)
   public List<Chassis> getChassisList() {
      return chassisService.getAllChassis();
   }

   /**
    * Creates a new chassis object.
    *
    * @param chassis  the new chassis.
    */
   @RequestMapping(method = RequestMethod.POST)
   public void create(@RequestBody final Chassis chassis) {
      chassisService.create(chassis);
   }

   /**
    * Edits a chassis object.
    *
    * @param chassis chassis object to be updated.
    */
   @RequestMapping(value = "/edit", method = RequestMethod.PUT)
   public void edit(@RequestBody final Chassis chassis) {
      chassisService.update(chassis);
   }

   /**
    * Deletes chassis objects.
    *
    * @param ids ids of chassis objects to be deleted.
    */
   @RequestMapping(value = "/delete", method = RequestMethod.DELETE)
   public void delete(@RequestParam final String[] ids) {
      for (final String targetId : ids) {
         chassisService.delete(targetId);
      }
   }

   /**
    * Deletes a chassis object.
    *
    * @param objectId id of chassis object to be deleted.
    */
   @RequestMapping(value = "/{objectId}", method = RequestMethod.DELETE)
   public void deleteChassisById(@PathVariable("objectId") final String objectId) {
      chassisService.delete(objectId);
   }
}

