/*
 * ******************************************************************
 * Copyright (c) 2020-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

export enum MessageType {
   chassisUpdated = 'CHASSIS_UPDATED'
}

export interface Message {
   type: MessageType;
}
