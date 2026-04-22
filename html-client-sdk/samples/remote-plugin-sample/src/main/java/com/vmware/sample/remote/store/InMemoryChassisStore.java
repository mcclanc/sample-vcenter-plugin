/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

package com.vmware.sample.remote.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vmware.sample.remote.exception.RemotePluginException;
import com.vmware.sample.remote.gateway.SessionServiceImpl;
import com.vmware.sample.remote.model.Chassis;
import com.vmware.sample.remote.store.exception.ExistingChassisObjectException;
import com.vmware.sample.remote.store.exception.ExistingHostRelationException;

import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.CollectionUtils;

/**
 * Simplified in-memory data store for the Chassis objects, and related utilities.
 * <p>
 * ***************************************************************************** *
 * IMPORTANT: this implementation uses in-memory data to keep the setup easy,    *
 * a real implementation should retrieve data from a remote server or database   *
 * and have very minimal or no storing/caching in the java layer because the     *
 * server must remain stateless and be able to scale out.                        *
 * ***************************************************************************** *
 * <p>
 * Note that this class is thread-safe but doesn't deal with complex operations
 * or large data sets. It is not intended to be used as-is!
 */
public class InMemoryChassisStore implements ChassisStore {
   private static final Log logger = LogFactory
         .getLog(SessionServiceImpl.class);
   // Default number of pre-defined chassis objects is 4.
   private static final int CHASSIS_INITIAL_COUNT = 4;

   private static final String CHASSIS_ID = "chassis:%s";
   private static final String CHASSIS_NAME = "Chassis %s";
   private static final String CHASSIS_SERVER_TYPE = "Server_Type %s";
   private static final String CHASSIS_DIMENSIONS = "20in x 30in x 17in";

   // Internal index used to create unique ids.
   private static int index = 0;

   // Map of chassis objects used in the sample.
   // The key is a chassis object's id and the value is the chassis object.
   private final Map<String, Chassis> store;

   public InMemoryChassisStore() {
      Map<String, Chassis> store = new HashMap<>(CHASSIS_INITIAL_COUNT);
      this.store = Collections.synchronizedMap(store);

      // Create an initial set of chassis objects.
      for (int i = 0; i < CHASSIS_INITIAL_COUNT; i++) {
         final Chassis newChassis = new Chassis();
         newChassis.name = String.format(CHASSIS_NAME, i);
         newChassis.serverType = String.format(CHASSIS_SERVER_TYPE, i);
         newChassis.dimensions = CHASSIS_DIMENSIONS;
         newChassis.isActive = false;
         create(newChassis);
      }
   }

   /**
    * Retrieves all chassis objects stored in the in-memory store.
    * NOTE: A real implementation should retrieve objects
    * data from a plugin back-end server.
    *
    * @return a list of chassis objects.
    */
   @Override
   public List<Chassis> getObjects() {
      return new ArrayList<>(store.values());
   }

   /**
    * Retrieves a chassis object by a given chassis id.
    *
    * @param id the id of the chassis object.
    * @return a chassis object for the given id
    * or null if such object does not exist.
    */
   @Override
   public Chassis getObjectById(final String id) {
      Validate.notNull(id);
      return store.get(id);
   }

   /**
    * Adds a new chassis object to the in-memory store.
    *
    * @param chassis chassis object.
    * @return the newly added chassis object.
    * @throws RemotePluginException if the name is duplicate.
    */
   @Override
   public Chassis create(final Chassis chassis) {
      if (isNameDuplicate(chassis.name)) {
         logger.error(String.format("Chassis with name %s already exists.",
               chassis.name));
         throw new ExistingChassisObjectException(
               "The chassis with name '" + chassis.name + "' already exists");
      }
      // Add the chassis object to the in-memory store.
      chassis.id = generateId();
      store.put(chassis.id, chassis);
      return chassis;
   }

   /**
    * Updates an existing chassis object.
    *
    * @param chassis the chassis object.
    * @return true if the chassis object was successfully updated
    * or false otherwise.
    * @throws RemotePluginException if the name is duplicate.
    */
   @Override
   public boolean update(final Chassis chassis) {
      final boolean checkForDuplicateName = !chassis.name
            .equals(Validate.notNull(getObjectById(chassis.id)).name);
      if (checkForDuplicateName && isNameDuplicate(chassis.name)) {
         logger.error(String.format("Chassis with name %s already exists.",
               chassis.name));
         throw new ExistingChassisObjectException(
               "The chassis with name '" + chassis.name + "' already exists");
      }
      final Chassis updatedChassis = store.put(chassis.id, chassis);
      return (updatedChassis != null);
   }

   /**
    * Deletes a chassis object from the in-memory store.
    *
    * @param id the id of the chassis object which will be removed.
    * @return the chassis object that was removed
    * or null if a chassis object with the given id does not exist.
    */
   @Override
   public Chassis delete(final String id) {
      if (id == null) {
         return null;
      }

      if (!CollectionUtils.isEmpty(store.get(id).relatedHostsIds)) {
         throw new ExistingHostRelationException(
               String.format("Chassis %s cannot be deleted because it has related hosts.", id));
      }

      return store.remove(id);
   }

   /**
    * Generates a chassis object id.
    *
    * @return a chassis object id.
    */
   private static String generateId() {
      return String.format(CHASSIS_ID, index++);
   }

   /**
    * Validates if the given chassis name is already in use.
    *
    * @param name the name of a chassis object.
    * @return true if the given chassis name is duplicate, or false otherwise.
    */
   private boolean isNameDuplicate(final String name) {
      synchronized (store) {
         for (final Chassis chassis : store.values()) {
            if (name.equals(chassis.name)) {
               return true;
            }
         }
         return false;
      }
   }
}
