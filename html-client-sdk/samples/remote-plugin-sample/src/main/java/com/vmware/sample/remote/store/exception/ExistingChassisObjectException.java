/*
 * ******************************************************************
 * Copyright (c) 2020-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote.store.exception;

/**
 * Exception representing a failure when a chassis object already exists.
 */
public class ExistingChassisObjectException extends RuntimeException {
   public ExistingChassisObjectException(final String message) {
      super(message);
   }
}
