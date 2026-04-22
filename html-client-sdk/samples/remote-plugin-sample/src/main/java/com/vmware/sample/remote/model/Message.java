/*
 * ******************************************************************
 * Copyright (c) 2020-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

package com.vmware.sample.remote.model;

public class Message {
   public final MessageType type;

   public Message(MessageType type) {
      this.type = type;
   }
}
