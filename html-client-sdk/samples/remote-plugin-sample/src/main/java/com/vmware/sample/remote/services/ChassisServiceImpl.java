/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

package com.vmware.sample.remote.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.vmware.sample.remote.gateway.GatewayCredentials;
import com.vmware.sample.remote.model.Chassis;
import com.vmware.sample.remote.model.Host;
import com.vmware.sample.remote.model.Message;
import com.vmware.sample.remote.model.MessageType;
import com.vmware.sample.remote.services.tasks.ChassisTaskType;
import com.vmware.sample.remote.services.tasks.faults.ChassisAlreadyExistsFault;
import com.vmware.sample.remote.services.tasks.faults.RelatedChassisDeletionFault;
import com.vmware.sample.remote.store.ChassisStore;
import com.vmware.sample.remote.store.exception.ExistingChassisObjectException;
import com.vmware.sample.remote.store.exception.ExistingHostRelationException;
import com.vmware.sample.remote.vim25.services.TaskService;
import com.vmware.vim25.InvalidStateFaultMsg;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.TaskInfo;

/**
 * This service can perform create/update/delete actions on chassis objects.
 * When an action is performed a task in the vCenter Server is created as well.
 * The created task is only a simulation of a long running task.
 * A real scenario of a long-running operation normally includes
 * making HTTP calls or accessing multiple services.
 */
public class ChassisServiceImpl implements ChassisService {
   // Log messages
   private static final String CHASSIS_OBJECT_DOES_NOT_EXIST_LOG_MSG =
         "Chassis object with the ID '%s' does not exist.";
   private static final String CHASSIS_OBJECT_CREATED_LOG_MSG =
         "Chassis object with the ID '%s' was successfully created: '%s'.";
   private static final String MISSING_TASK_IDS_MESSAGE =
         "Could not create a vCenter Server task. " +
               "This is possibly due to plug-in extension registration " +
               "not containing tasks, faults and resources.";

   private static final Log logger = LogFactory.getLog(
         ChassisServiceImpl.class);

   private final ExecutorService executorService = Executors.newFixedThreadPool(10);

   private final ChassisStore inMemoryChassisStore;
   private final MessagingService messagingService;
   private final TaskService taskService;

   public ChassisServiceImpl(final ChassisStore inMemoryChassisStore,
         final MessagingService messagingService,
         final TaskService taskService) {
      this.inMemoryChassisStore = inMemoryChassisStore;
      this.messagingService = messagingService;
      this.taskService = taskService;
   }

   @Override
   public Chassis getChassisById(final String chassisId) {
      final Chassis chassis = inMemoryChassisStore.getObjectById(chassisId);
      return Validate.notNull(chassis, CHASSIS_OBJECT_DOES_NOT_EXIST_LOG_MSG);
   }

   @Override
   public List<Chassis> getAllChassis() {
      return inMemoryChassisStore.getObjects();
   }

   /**
    * The creation of chassis object is done with a simulation of a long running task.
    * Namely:
    *    - a task in the vCenter Server is created.
    *    - then the chassis object is created.
    *    - and after that the task is completed.
    * This long simulation of a long running task is done in a separate thread.
    * This is needed to allow the modal dialog in the frontend to be closed immediately
    * after the completion of the modal form.
    *
    * @param chassis chassis data used to create a new chassis object.
    */
   @Override
   public void create(final Chassis chassis) {
      Validate.notNull(chassis);
      Validate.notNull(chassis.name);

      final GatewayCredentials credentials = getCredentialsFromRequest();
      executorService.submit(() -> createTask(chassis, credentials));
   }

   /**
    * The update of chassis object is done with a simulation of a long running task.
    * Namely:
    *    - a task in the vCenter Server is created.
    *    - then the chassis object is updated.
    *    - and after that the task is completed.
    * This long simulation of a long running task is done in a separate thread.
    * This is needed to allow the modal dialog in the frontend to be closed immediately
    * after the completion of the modal form.
    *
    * @param chassis    chassis data used to update a chassis object with.
    */
   @Override
   public void update(final Chassis chassis) {
      Validate.notNull(chassis);

      final GatewayCredentials credentials = getCredentialsFromRequest();
      executorService.submit(() -> updateTask(chassis, credentials));
   }

   /**
    * The deletion of chassis object is done with a simulation of a long running task.
    * Namely:
    *    - a task in the vCenter Server is created.
    *    - then if the chassis object is not related to any host:
    *       - the chassis object is deleted.
    *       - the task is completed.
    *    - else if the chassis object is related to at least one host:
    *       - the task is finished as failed.
    * This long simulation of a long running task is done in a separate thread.
    * This is needed to allow the modal dialog in the frontend to be closed immediately.
    * after the completion of the modal form.
    *
    * @param chassisId   the id of a chassis object.
    */
   @Override
   public void delete(final String chassisId) {
      final GatewayCredentials credentials = getCredentialsFromRequest();
      executorService.submit(() -> deleteTask(chassisId, credentials));
   }

   @Override
   public void setRelatedHosts(final String chassisId, final List<String> relatedHostsIds) {
      final Chassis chassis = getChassisById(chassisId);
      if (Objects.equals(chassis.relatedHostsIds, relatedHostsIds)) {
         return;
      }

      chassis.relatedHostsIds = relatedHostsIds;

      messagingService.broadcastMessage(new Message(MessageType.CHASSIS_UPDATED));
   }

   @Override
   public void updateHostRelation(final Host host) {
      final String hostId = host.id;
      final List<String> updatedChassisIds = host.relatedChassisIds;
      final List<Chassis> allChassis = getAllChassis();
      for (final Chassis currentChassis : allChassis) {
         final String currentChassisId = currentChassis.id;
         final List<String> currentChassisHostIds = currentChassis.relatedHostsIds;
         // Add to relation
         if ((CollectionUtils.isEmpty(currentChassisHostIds)
               || !currentChassisHostIds.contains(hostId)) && updatedChassisIds
               .contains(currentChassisId)) {
            addRelatedHost(currentChassisId, hostId);
            // Remove from relation
         } else if (!CollectionUtils.isEmpty(currentChassisHostIds)
               && currentChassisHostIds.contains(hostId) && !updatedChassisIds
               .contains(currentChassisId)) {
            removeRelatedHost(currentChassisId, hostId);
         }
      }

      messagingService.broadcastMessage(new Message(MessageType.CHASSIS_UPDATED));
   }

   @Override
   public List<Chassis> getRelatedChassis(final String hostId) {
      final List<Chassis> allChassis = getAllChassis();
      if (CollectionUtils.isEmpty(allChassis)) {
         return Collections.emptyList();
      }
      return allChassis.stream().filter(
            chassis -> !CollectionUtils.isEmpty(chassis.relatedHostsIds)
                  && chassis.relatedHostsIds.contains(hostId))
            .collect(Collectors.toList());
   }

   private GatewayCredentials getCredentialsFromRequest() {
      final HttpServletRequest servletRequest =
            ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                  .getRequest();
      return GatewayCredentials.fromRequestHeaders(servletRequest);
   }

   private void createTask(final Chassis chassis, final GatewayCredentials credentials) {
      TaskInfo task = null;
      try {
         task = taskService.createTask(credentials, ChassisTaskType.CREATE.getTaskId(), chassis.name);
      } catch (final RuntimeFaultFaultMsg | InvalidStateFaultMsg ex) {
         logger.error(
               "Could not create a vCenter Server task for chassis creation.",
               ex);
      } catch (final RuntimeException ex) {
         logger.error(MISSING_TASK_IDS_MESSAGE, ex);
      }

      final Chassis newChassis;
      try {
         newChassis = inMemoryChassisStore.create(chassis);
      } catch (final ExistingChassisObjectException existingChassisObjectException) {
         logger.warn("Could not create chassis.", existingChassisObjectException);

         if (task != null) {
            try {
               taskService.failTask(credentials, task, new ChassisAlreadyExistsFault());
            } catch (RuntimeFaultFaultMsg | InvalidStateFaultMsg ex1) {
               logger.error(
                     "Could not fail the vCenter Server Task for chassis deletion: " +
                           chassis.id,existingChassisObjectException);
            }
         }
         return;
      }

      logger.info(String.format(CHASSIS_OBJECT_CREATED_LOG_MSG, newChassis.id,
            newChassis.toString()));

      messagingService.broadcastMessage(new Message(MessageType.CHASSIS_UPDATED));
      try {
         if (task != null) {
            taskService.succeedTask(credentials, task);
         }
      } catch (final RuntimeFaultFaultMsg | InvalidStateFaultMsg ex) {
         logger.error(
               "Could not update the vCenter Server task for chassis creation: " +
                     newChassis.id,
               ex);
      } catch (final RuntimeException ex) {
         logger.error(MISSING_TASK_IDS_MESSAGE, ex);
      }
   }

   private void updateTask(final Chassis chassis, final GatewayCredentials credentials) {
      TaskInfo task = null;
      try {
         task = taskService.createTask(credentials, ChassisTaskType.UPDATE.getTaskId(),
               chassis.name);
      } catch (final RuntimeFaultFaultMsg | InvalidStateFaultMsg ex) {
         logger.error(
               "Could not create a vCenter Server Task for chassis update: " +
                     chassis.id,
               ex);
      } catch (final RuntimeException ex) {
         logger.error(MISSING_TASK_IDS_MESSAGE, ex);
      }

      try {
         inMemoryChassisStore.update(chassis);
      } catch (final ExistingChassisObjectException existingChassisObjectException) {
         logger.warn("Could not update chassis.", existingChassisObjectException);

         if (task != null) {
            try {
               taskService.failTask(credentials, task, new ChassisAlreadyExistsFault());
            } catch (final RuntimeFaultFaultMsg | InvalidStateFaultMsg ex1) {
               logger.error(
                     "Could not fail the vCenter Server Task for chassis deletion: " +
                           chassis.id, existingChassisObjectException);
            }
         }
         return;
      }

      messagingService.broadcastMessage(new Message(MessageType.CHASSIS_UPDATED));

      try {
         if (task != null) {
            taskService.succeedTask(credentials, task);
         }
      } catch (final RuntimeFaultFaultMsg | InvalidStateFaultMsg ex) {
         logger.error(
               "Could not update the vCenter Server Task for chassis update: " +
                     chassis.id,
               ex);
      } catch (final RuntimeException ex) {
         logger.error(MISSING_TASK_IDS_MESSAGE, ex);
      }
   }

   private void deleteTask(final String chassisId, final GatewayCredentials credentials) {
      TaskInfo task = null;
      try {
         task = taskService.createTask(credentials, ChassisTaskType.DELETE.getTaskId(),
               inMemoryChassisStore.getObjectById(chassisId).name);
      } catch (final RuntimeFaultFaultMsg | InvalidStateFaultMsg ex) {
         logger.error(
               "Could not create a vCenter Server Task for chassis deletion: " + chassisId,
               ex);
      } catch (final RuntimeException ex) {
         logger.error(MISSING_TASK_IDS_MESSAGE, ex);
      }

      final Chassis chassis;
      try {
         chassis = inMemoryChassisStore.delete(chassisId);
      } catch (final ExistingHostRelationException ex) {
         logger.warn("Could not delete chassis.", ex);

         if (task != null) {
            try {
               taskService.failTask(credentials, task, new RelatedChassisDeletionFault());
            } catch (RuntimeFaultFaultMsg | InvalidStateFaultMsg ex1) {
               logger.error(
                     "Could not fail the vCenter Server Task for chassis deletion: " +
                           chassisId,
                     ex);
            }
         }
         return;
      }

      Validate.notNull(chassis);
      messagingService.broadcastMessage(new Message(MessageType.CHASSIS_UPDATED));

      if (task != null) {
         try {
            taskService.succeedTask(credentials, task);
         } catch (RuntimeFaultFaultMsg | InvalidStateFaultMsg ex) {
            logger.error(
                  "Could not update the vCenter Server Task for chassis deletion: " +
                        chassisId,
                  ex);
         }
      }
   }

   private void addRelatedHost(final String chassisId, final String hostId) {
      if (CollectionUtils.isEmpty(getChassisById(chassisId).relatedHostsIds)) {
         getChassisById(chassisId).relatedHostsIds = new ArrayList<>();
      }
      getChassisById(chassisId).relatedHostsIds.add(hostId);
   }

   private void removeRelatedHost(String chassisId, String hostId) {
      getChassisById(chassisId).relatedHostsIds.remove(hostId);
   }
}
