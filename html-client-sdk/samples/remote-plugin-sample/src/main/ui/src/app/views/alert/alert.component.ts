/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
import { Component, Input } from '@angular/core';

@Component({
   selector: 'app-alert-message',
   templateUrl: './alert.component.html',
   standalone: true,
})
export class AlertComponent {
   @Input()
   error?: Error;

   onAlertClose(): void {
      this.error = undefined;
   }
}
