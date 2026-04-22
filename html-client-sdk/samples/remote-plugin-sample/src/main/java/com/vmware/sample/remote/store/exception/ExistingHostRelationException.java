/*
 * ******************************************************************
 * Copyright (c) 2020-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote.store.exception;

/**
 * Exception representing a failure when a chassis object has active relations with
 * vCenter Server Host objects.
 */
public class ExistingHostRelationException extends RuntimeException {
   public ExistingHostRelationException(final String message) {
      super(message);
   }
}
