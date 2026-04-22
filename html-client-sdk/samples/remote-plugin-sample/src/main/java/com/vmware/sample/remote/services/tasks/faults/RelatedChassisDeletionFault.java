/*
 * ******************************************************************
 * Copyright (c) 2020-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote.services.tasks.faults;

import com.vmware.vim25.LocalizableMessage;
import com.vmware.vim25.VimFault;

/**
 * Localizable Fault which is displayed when an attempt is made to delete a chassis
 * object which has related hosts.
 * In order for this fault to be found when a task is created,
 * it must be provided as part of the plug-in registration in the ExtensionManager.
 * The fault should have been register with ID:
 *    "com.vmware.sample.remote.1.0.0.0.faults.RelatedChassisDeletionFault".
 */
public class RelatedChassisDeletionFault extends VimFault {
   public RelatedChassisDeletionFault() {
      this.faultMessage = getFaultMessage();
      final LocalizableMessage msg = new LocalizableMessage();
      msg.setKey("com.vmware.sample.remote.1.0.0.0.faults.RelatedChassisDeletionFault.summary");

      this.faultMessage.add(msg);
   }
}
