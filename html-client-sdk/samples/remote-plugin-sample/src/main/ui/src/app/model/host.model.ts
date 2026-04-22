/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
export class Host {

   public static empty(): Host {
      return new Host('', '', '', '', '', '', []);
   }

   constructor(
      public id: string,
      public name: string,
      public vCenterName: string,
      public state: string,
      public numCpus: string,
      public memorySize: string,
      public relatedChassisIds: string[],
   ) {}
}
