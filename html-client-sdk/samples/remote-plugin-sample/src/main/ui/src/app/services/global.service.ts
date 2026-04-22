/*
 * ******************************************************************
 * Copyright (c) 2018-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

import { Injectable } from '@angular/core';

/* eslint-disable @typescript-eslint/no-explicit-any */

@Injectable({ providedIn: 'root' })
export class GlobalService {
   public htmlClientSdk: any;

   constructor() {
      this.htmlClientSdk = (window as any).htmlClientSdk;
   }
}
