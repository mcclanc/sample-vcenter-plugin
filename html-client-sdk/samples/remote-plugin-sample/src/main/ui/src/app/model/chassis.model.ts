/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

export class Chassis {
   static readonly DEFAULT_CHASSIS_PAGE_SIZE = 10;
   static readonly PROP_CHASSIS_PAGE_SIZE = 'com.vmware.samples.remote.numberChassisPerPage';

   public static empty(): Chassis {
      return new Chassis('', '', '', '', false, 0, 0, []);
   }

   public static clone(chassis: Chassis): Chassis {
      return new Chassis(
         chassis.id,
         chassis.name,
         chassis.dimensions,
         chassis.serverType,
         chassis.isActive,
         chassis.healthStatus,
         chassis.complianceStatus,
         chassis.relatedHostsIds,
      );
   }

   constructor(
      public id: string,
      public name: string,
      public dimensions: string,
      public serverType: string,
      public isActive: boolean,
      public healthStatus: number,
      public complianceStatus: number,
      public relatedHostsIds: string[],
   ) {}
}
