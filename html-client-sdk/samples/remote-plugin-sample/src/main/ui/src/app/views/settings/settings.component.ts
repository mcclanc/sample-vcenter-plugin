/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ClarityModule } from '@clr/angular';
import { TranslateModule } from '@ngx-translate/core';
import { Chassis } from '~models/chassis.model';

@Component({
   templateUrl: './settings.component.html',
   styleUrl: './settings.component.scss',
   standalone: true,
   imports: [ClarityModule, FormsModule, TranslateModule],
})
export class SettingsComponent implements OnInit {
   readonly minPageSize = 0;
   readonly maxPageSize = 20;

   numberOfChassisPerPage = Chassis.DEFAULT_CHASSIS_PAGE_SIZE;
   numberOfChassisPerPageMessageValue = Chassis.DEFAULT_CHASSIS_PAGE_SIZE;
   showSuccessMessage = false;

   ngOnInit(): void {
      this.numberOfChassisPerPage = this.parsePageSize(localStorage.getItem(Chassis.PROP_CHASSIS_PAGE_SIZE));
   }

   /**
    * Triggered when user clicks on 'Update' button.
    */
   onUpdate() {
      this.numberOfChassisPerPage = this.parsePageSize(this.numberOfChassisPerPage.toString());
      this.numberOfChassisPerPageMessageValue = this.numberOfChassisPerPage;
      this.setNumberChassisPerPageInLocalStorage(this.numberOfChassisPerPage);
      this.showSuccessMessage = true;
   }

   /**
    * Sets the new value in the local storage.
    *
    * @param numberChassisPerPage -
    * number of chassis displayed in the chassis list per page.
    */
   private setNumberChassisPerPageInLocalStorage(numberChassisPerPage: number) {
      localStorage.setItem(
            Chassis.PROP_CHASSIS_PAGE_SIZE, numberChassisPerPage.toString());
   }

   private parsePageSize(text?: string | null): number {
      const value = Number.parseInt(text ?? '', 10);
      return Number.isFinite(value) && value > this.minPageSize && value < this.maxPageSize
         ? value
         : Chassis.DEFAULT_CHASSIS_PAGE_SIZE;
   }
}
