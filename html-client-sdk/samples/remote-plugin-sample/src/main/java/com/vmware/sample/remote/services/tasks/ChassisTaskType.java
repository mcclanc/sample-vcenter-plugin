/*
 * ******************************************************************
 * Copyright (c) 2020-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote.services.tasks;

/**
 * ChassisTaskType contains information about the task IDs
 * which are expected to have been provided with the plug-in registration.
 *
 * Trying to create a vCenter Server task with a given ID will
 * fail if the task ID is missing from the Extension Manager.
 */
public enum ChassisTaskType {
   CREATE("com.vmware.sample.remote.1.0.0.0.CreateChassis"),
   UPDATE("com.vmware.sample.remote.1.0.0.0.UpdateChassis"),
   DELETE("com.vmware.sample.remote.1.0.0.0.DeleteChassis");

   ChassisTaskType(final String taskId) {
      this.taskId = taskId;
   }

   public String getTaskId() {
      return taskId;
   }

   final String taskId;
}
