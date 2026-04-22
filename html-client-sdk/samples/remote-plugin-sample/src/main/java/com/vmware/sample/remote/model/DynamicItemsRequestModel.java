/*
 * ******************************************************************
 * Copyright (c) 2020-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model to capture the request information sent by the vSphere Client when retrieving
 * the dynamic views/actions
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DynamicItemsRequestModel {

   @JsonProperty("apiVersion")
   public String apiVersion;

   @JsonProperty("objectIds")
   public List<String> objectIds;

   @JsonProperty("locale")
   public String locale;
}
