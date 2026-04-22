/*
 * ******************************************************************
 * Copyright (c) 2018-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

import { Component, OnInit} from '@angular/core';
import { Chassis } from '~models/chassis.model';
import { ChassisService } from '~services/chassis.service';
import { HostsService } from '~services/hosts.service';
import { Host } from '~models/host.model';
import { GlobalService } from '~services/global.service';
import { TranslateModule } from '@ngx-translate/core';
import { ClarityModule } from '@clr/angular';
import { StatusComponent } from '../status/status.component';

@Component({
   templateUrl: './host.monitor.component.html',
   standalone: true,
   imports: [ClarityModule, TranslateModule, StatusComponent],
})
export class HostMonitorComponent implements OnInit {

   public chassisList: Chassis[] = [];
   public loading: boolean = true;
   public numberOfChassisPerPage: number =
         Chassis.DEFAULT_CHASSIS_PAGE_SIZE;
   private contextObjectId!: string;
   private _selectedChassis?: Chassis[];

   constructor(private chassisService: ChassisService,
         private hostsService: HostsService,
         private globalService: GlobalService) {
   }

   ngOnInit(): void {
      const value = parseInt(
            localStorage.getItem(Chassis.PROP_CHASSIS_PAGE_SIZE) ?? '', 10);
      if (Number.isFinite(value) && value > 0) {
         this.numberOfChassisPerPage = value;
      }
      this.contextObjectId =
            this.globalService.htmlClientSdk.app.getContextObjects()[0].id;
      this.loadData();
   }

   get selectedChassis() {
      return this._selectedChassis;
   }

   set selectedChassis(chassis: Chassis[] | undefined) {
      this._selectedChassis = chassis;
   }

   public updateChassisRelation() {
      this.loading = true;
      const host = Host.empty();
      host.id = this.contextObjectId;
      host.relatedChassisIds = this.selectedChassis?.map(chassis => chassis.id) ?? [];
      this.hostsService.edit(host).subscribe({
         error: () => this.loading = false,
         complete: () => this.loading = false
      });
   }

   onContextMenu(): boolean {
      return false;
   }

   navigateToChassis(selectedChassis: Chassis): void {
      this.globalService.htmlClientSdk.app.navigateTo({
         targetViewId: 'entryPoint',
         customData: {
            navigationPath: '/entry-point/list',
            selectedChassis: [selectedChassis]
         }
      });
   }

   private loadData(): void {
      this.chassisService.getAllChassis().subscribe((chassis: Chassis[]) => {
         this.chassisList = chassis;
         this.selectedChassis = this.filterRelatedChassis();
         this.loading = false;
      });
   }

   private filterRelatedChassis(): Chassis[] | undefined {
      if (!this.chassisList?.length) {
         return undefined;
      }
      return this.chassisList.filter((chassis: Chassis) => !!chassis.relatedHostsIds &&
               chassis.relatedHostsIds.indexOf(this.contextObjectId) > -1);
   }
}
