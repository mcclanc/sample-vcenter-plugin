/*
 * ******************************************************************
 * Copyright (c) 2020-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote.vim25.services;

import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.vmware.sample.remote.gateway.GatewayCredentials;
import com.vmware.sample.remote.gateway.SessionService;
import com.vmware.sample.remote.gateway.VimSessionInfo;
import com.vmware.vim25.InvalidStateFaultMsg;
import com.vmware.vim25.LocalizableMessage;
import com.vmware.vim25.LocalizedMethodFault;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.MethodFault;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.VimPortType;

/**
 * Task service used to create and update tasks in the vCenter Server.
 */
public class TaskService {
   private static final String PROP_SERVICE_INSTANCE = "ServiceInstance";

   private static final Log logger = LogFactory.getLog(
         TaskService.class);

   private final SessionService sessionService;
   private final ManagedObjectReference serviceInstanceRef = new ManagedObjectReference();

   public TaskService(final SessionService sessionService) {
      this.sessionService = sessionService;
      serviceInstanceRef.setType(PROP_SERVICE_INSTANCE);
      serviceInstanceRef.setValue(PROP_SERVICE_INSTANCE);
   }

   public TaskInfo createTask(final GatewayCredentials credentials, final String taskId, final String descriptionMsg) throws
         RuntimeFaultFaultMsg, InvalidStateFaultMsg {
      Validate.notEmpty(taskId);

      final VimSessionInfo vimSessionInfo = sessionService.getVimSessionInfo(credentials);
      final VimPortType vimPort = vimSessionInfo.getVimPort();
      final ServiceContent serviceContent = vimPort.retrieveServiceContent(
            serviceInstanceRef);

      final ManagedObjectReference taskMgr = serviceContent.getTaskManager();
      final ManagedObjectReference rootFolder = serviceContent.getRootFolder();

      final TaskInfo task = vimPort.createTask(taskMgr, rootFolder, taskId,
            vimSessionInfo.getUserSession().getUserName(), false, null, null);

      updateState(credentials, task, TaskInfoState.RUNNING, null);
      updateDescription(credentials, task, descriptionMsg);

      logger.info(String.format("Created task with task id: %s, and description: %s",
            taskId, descriptionMsg));
      return task;
   }

   public void succeedTask(final GatewayCredentials credentials, final TaskInfo task) throws
         RuntimeFaultFaultMsg, InvalidStateFaultMsg {
      updateState(credentials, task, TaskInfoState.SUCCESS, null);
   }

   public void failTask(final GatewayCredentials credentials, final TaskInfo task, final MethodFault fault) throws
         InvalidStateFaultMsg, RuntimeFaultFaultMsg {
      Validate.notNull(task);
      updateState(credentials, task, TaskInfoState.ERROR, fault);
   }

   public void updateDescription(final GatewayCredentials credentials, final TaskInfo task, final String descriptionMsg) throws
         RuntimeFaultFaultMsg {
      final VimPortType vimPort = sessionService.getVimSessionInfo(credentials).getVimPort();
      final LocalizableMessage locMsg = new LocalizableMessage();
      locMsg.setKey(task.getKey() + ".details");
      locMsg.setMessage(descriptionMsg);
      task.setDescription(locMsg);
      vimPort.setTaskDescription(task.getTask(), locMsg);
   }

   public void updateState(final GatewayCredentials credentials, final TaskInfo task, final TaskInfoState state,
         final MethodFault fault) throws
         InvalidStateFaultMsg, RuntimeFaultFaultMsg {
      final VimPortType vimPort = sessionService.getVimSessionInfo(credentials).getVimPort();
      if (fault == null) {
         vimPort.setTaskState(task.getTask(), state, null, null);
      } else {
         final LocalizedMethodFault localized = new LocalizedMethodFault();
         localized.setFault(fault);
         task.setError(localized);
         vimPort.setTaskState(task.getTask(), state, null, localized);
      }
      task.setState(state);
   }
}
