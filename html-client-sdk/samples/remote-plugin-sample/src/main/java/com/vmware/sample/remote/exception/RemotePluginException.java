/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote.exception;

/**
 * An exception thrown in case of expected failures.
 * localeKey property is the key to use when localizing the error messages
 * in the UI.
 */
public class RemotePluginException extends RuntimeException {

   private String localeKey;

   public RemotePluginException(String localeKey) {
      super();
      this.localeKey = localeKey;
   }

   public String getLocaleKey() {
      return localeKey;
   }
}
