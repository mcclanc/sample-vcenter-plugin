/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

import { Component, OnInit } from '@angular/core';
import { ChassisService } from '~services/chassis.service';
import { Chassis } from '~models/chassis.model';
import { GlobalService } from '~services/global.service';
import { ClarityModule } from '@clr/angular';

@Component({
   templateUrl: './host-card.component.html',
   styleUrls: ['./host-card.component.scss'],
   standalone: true,
   imports: [ClarityModule],
})
export class HostCardComponent implements OnInit {
   private static readonly HOST_MONITOR_VIEW_NAVIGATION_ID = 'hostMonitor';

   public loading = true;
   public numberOfRelatedChassis = 0;
   private contextObjectId!: string;

   constructor(private chassisService: ChassisService,
         private globalService: GlobalService) {
   }

   ngOnInit(): void {
      this.contextObjectId =
            this.globalService.htmlClientSdk.app.getContextObjects()[0].id;
      this.loadData();
   }

   public navigateToHostMonitorView(): void {
      const navigateParams = {
         targetViewId: HostCardComponent.HOST_MONITOR_VIEW_NAVIGATION_ID,
         objectId: this.contextObjectId
      };
      this.globalService.htmlClientSdk.app.navigateTo(navigateParams);
   }

   private loadData(): void {
      this.chassisService.getRelatedChassis(this.contextObjectId)
            .subscribe((chassis: Chassis[]) => {
               this.numberOfRelatedChassis = chassis.length;
               this.loading = false;
            });
   }
}
