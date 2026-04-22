/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

import { Component, OnInit, ViewChild } from '@angular/core';
import { ClarityModule, ClrWizard, ClrWizardPage } from '@clr/angular';
import {
   FormControl,
   FormGroup,
   ReactiveFormsModule,
   Validators
} from '@angular/forms';
import { Chassis } from '~models/chassis.model';
import { ChassisService } from '~services/chassis.service';
import { GlobalService } from '~services/global.service';
import { TranslateModule } from '@ngx-translate/core';
import { AlertComponent } from 'app/views/alert/alert.component';
import { HostListComponent } from 'app/views/tabs/hosts/hosts-list.component';
import { StatusComponent } from 'app/views/status/status.component';

@Component({
   templateUrl: './create-wizard.component.html',
   styleUrls: ['./create-wizard.component.scss'],
   standalone: true,
   imports: [
      ClarityModule,
      ReactiveFormsModule,
      TranslateModule,
      AlertComponent,
      HostListComponent,
      StatusComponent,
   ],
})
export class CreateWizardComponent implements OnInit {
   @ViewChild('wizard') wizard!: ClrWizard;
   @ViewChild('myFinishPage') finishPage!: ClrWizardPage;

   error?: Error;
   readyToCompleteData?: {
      name: string,
      serverType: string,
      dimensions: string
      state: boolean,
      hosts: string[],
   };
   chassis: Chassis;
   wizardPage1Form!: FormGroup;
   wizardPage2Form!: FormGroup;

   constructor(
      private readonly chassisService: ChassisService,
      private readonly globalService: GlobalService,
   ) {
      this.chassis = Chassis.empty();
   }

   ngOnInit(): void {
      this.wizardPage1Form = new FormGroup({
         name: new FormControl(this.chassis.name, [Validators.required]),
         serverType: new FormControl(this.chassis.serverType)
      });
      this.wizardPage2Form = new FormGroup({
         dimensions: new FormControl(this.chassis.dimensions),
         isActive: new FormControl(this.chassis.isActive)
      });
   }

   loadReadyToCompletePageData(): void {
      Object.assign(this.chassis, this.wizardPage1Form.value, this.wizardPage2Form.value);
      this.readyToCompleteData = {
         name: this.chassis.name,
         serverType: this.formatEmptyOrNullValue(this.chassis.serverType),
         dimensions: this.formatEmptyOrNullValue(this.chassis.dimensions),
         state: this.chassis.isActive,
         hosts: this.chassis.relatedHostsIds
      };
   }

   onSubmit(): void {
      this.finishPage.completed = true;
      Object.assign(this.chassis, this.wizardPage1Form.value, this.wizardPage2Form.value);
      console.log(this.chassis);
      this.chassisService.create(this.chassis).subscribe({
         error: () => this.onCancel(),
         complete: () => this.onCancel()
      });
   }

   onGoBack(): void {
      this.wizard.previous();
   }

   onCancel(): void {
      this.wizard.close();
      this.onClose();
   }

   onClose(): void {
      this.globalService.htmlClientSdk.modal.close();
   }

   isNameEmpty() {
      const control = this.wizardPage1Form.get('name');
      return Boolean(control?.invalid && (control.dirty || control.touched));
   }

   isNamePristine() {
      const control = this.wizardPage1Form.get('name');
      return Boolean(control?.pristine);
   }

   onHostsSelectionChange(selectedHosts: string[]): void {
      this.chassis.relatedHostsIds = selectedHosts;
   }

   onNavigateToHostObject(): void {
      this.onCancel();
   }

   onError(error: Error): void {
      const message = error.message || 'An error occurred! Please read the logs for more information';
      this.error = new Error(message);
   }

   clearErrorMsg() {
      this.error = undefined;
   }

   private formatEmptyOrNullValue(value: string): string {
      if (typeof value !== 'string' || value.trim() === '') {
         return '--';
      }
      return value;
   }
}
