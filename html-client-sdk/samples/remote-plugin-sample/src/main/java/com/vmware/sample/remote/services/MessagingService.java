/*
 * ******************************************************************
 * Copyright (c) 2020-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

package com.vmware.sample.remote.services;

import com.vmware.sample.remote.model.Message;

/**
 * Interface used to send messages to WebSocket clients.
 */
public interface MessagingService {

   /**
    * Broadcasts a message to all clients.
    * @param message to send.
    */
   void broadcastMessage(final Message message);
}
