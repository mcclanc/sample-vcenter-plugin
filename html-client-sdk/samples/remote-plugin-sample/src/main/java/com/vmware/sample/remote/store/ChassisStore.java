/*
 * ******************************************************************
 * Copyright (c) 2022-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
package com.vmware.sample.remote.store;

import com.vmware.sample.remote.model.Chassis;

import java.util.List;

public interface ChassisStore {

   List<Chassis> getObjects();

   Chassis getObjectById(String id);

   Chassis create(Chassis chassis);

   boolean update(Chassis chassis);

   Chassis delete(String id);
}
