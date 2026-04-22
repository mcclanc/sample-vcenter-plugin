/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

import { Component, Input } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { Chassis } from '~models/chassis.model';

@Component({
   selector: 'app-monitor-view',
   templateUrl: './monitor.component.html',
   standalone: true,
   imports: [TranslateModule],
})
export class MonitorComponent {

   @Input()
   chassis!: Chassis;
}
