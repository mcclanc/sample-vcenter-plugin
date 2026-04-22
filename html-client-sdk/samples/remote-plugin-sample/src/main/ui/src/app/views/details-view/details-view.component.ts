/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

import {
   Component, Input
} from '@angular/core';
import { ClarityModule } from '@clr/angular';
import { TranslateModule } from '@ngx-translate/core';
import { Chassis } from '~models/chassis.model';
import { HeaderComponent } from '../header/header.component';
import { SummaryComponent } from '../tabs/summary/summary.component';
import { MonitorComponent } from '../tabs/monitor/monitor.component';
import { HostListComponent } from '../tabs/hosts/hosts-list.component';

@Component({
   selector: 'app-details-view',
   templateUrl: './details-view.component.html',
   styleUrls: ['./details-view.component.scss'],
   standalone: true,
   imports: [
      ClarityModule,
      TranslateModule,
      HeaderComponent,
      SummaryComponent,
      MonitorComponent,
      HostListComponent,
   ],
})
export class DetailsViewComponent {

   @Input()
   chassis!: Chassis;
}
