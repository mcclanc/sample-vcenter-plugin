/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

import { Component, Input } from '@angular/core';
import { ClarityModule, ClrIconModule } from '@clr/angular';
import { TranslateModule } from '@ngx-translate/core';

@Component({
   selector: 'app-chassis-status',
   templateUrl: './status.component.html',
   standalone: true,
   imports: [TranslateModule, ClarityModule, ClrIconModule],
})
export class StatusComponent {
   @Input()
   isActive = false;
}
