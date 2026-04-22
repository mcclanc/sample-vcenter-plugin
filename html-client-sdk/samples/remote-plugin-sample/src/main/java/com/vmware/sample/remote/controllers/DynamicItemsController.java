/*
 * ******************************************************************
 * Copyright (c) 2020-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote.controllers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.vmware.sample.remote.model.DynamicItem;
import com.vmware.sample.remote.model.DynamicItemsRequestModel;
import com.vmware.sample.remote.model.PluginServerDynamicItemsResponse;
import com.vmware.sample.remote.vim25.services.AuthorizationService;

/**
 * Responsible for providing public endpoints which will be called by the vSphere Client
 * in order to retrieve information about which views/actions/cards should be visible in the UI
 */
@RestController
@RequestMapping(value = "/dynamicItems",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
public class DynamicItemsController {
   private static final String MANAGE_VM_PRIVILEGE = "com.vmware.sample.remote.1.0.0.0.ManageVm";
   private final AuthorizationService authorizationService;

   public DynamicItemsController(final AuthorizationService authorizationService) {
      this.authorizationService = authorizationService;
   }

   @RequestMapping(value = "/vm/actions")
   public PluginServerDynamicItemsResponse retrieveVmActions(
         @RequestBody DynamicItemsRequestModel payload) {
      final boolean hasPrivilege = authorizationService
            .hasPrivilege(payload.objectIds, MANAGE_VM_PRIVILEGE);

      final List<DynamicItem> dynamicItems = new ArrayList<>();
      dynamicItems.add(new DynamicItem("vmAction", hasPrivilege));

      return new PluginServerDynamicItemsResponse("1.0.0", dynamicItems);
   }

   @RequestMapping(value = "/vm/configure")
   public PluginServerDynamicItemsResponse retrieveVmConfigureViews(
         @RequestBody DynamicItemsRequestModel payload) {
      final boolean hasPrivilege = authorizationService
            .hasPrivilege(payload.objectIds, MANAGE_VM_PRIVILEGE);

      final List<DynamicItem> dynamicItems = new ArrayList<>();
      dynamicItems.add(new DynamicItem("vmConfigureView", hasPrivilege));

      return new PluginServerDynamicItemsResponse("1.0.0", dynamicItems);
   }

   @RequestMapping(value = "/vm/summary")
   public PluginServerDynamicItemsResponse retrieveVmSummaryViews(
         @RequestBody DynamicItemsRequestModel payload) {
      final boolean hasPrivilege = authorizationService
            .hasPrivilege(payload.objectIds, MANAGE_VM_PRIVILEGE);

      final List<DynamicItem> dynamicItems = new ArrayList<>();
      dynamicItems.add(new DynamicItem("vmCard", hasPrivilege));

      return new PluginServerDynamicItemsResponse("1.0.0", dynamicItems);
   }
}
