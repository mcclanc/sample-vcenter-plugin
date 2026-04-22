/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

import { Component, Input } from '@angular/core';
import { ClarityModule } from '@clr/angular';
import { TranslateModule } from '@ngx-translate/core';
import { StatusComponent } from 'app/views/status/status.component';
import { Chassis } from '~models/chassis.model';

@Component({
   selector: 'app-summary-view',
   templateUrl: './summary.component.html',
   standalone: true,
   imports: [ClarityModule, TranslateModule, StatusComponent],
})
export class SummaryComponent {
   @Input()
   chassis!: Chassis;
}
