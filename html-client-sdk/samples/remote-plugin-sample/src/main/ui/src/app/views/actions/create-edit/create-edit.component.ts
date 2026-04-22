/*
 * ******************************************************************
 * Copyright (c) 2018-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

import { Component, OnInit } from '@angular/core';
import { Chassis } from '~models/chassis.model';
import { ChassisService } from '~services/chassis.service';
import { ActivatedRoute } from '@angular/router';
import {
   FormControl,
   FormGroup,
   ReactiveFormsModule,
   Validators
} from '@angular/forms';
import { GlobalService } from '~services/global.service';
import { ClarityModule } from '@clr/angular';
import { AlertComponent } from 'app/views/alert/alert.component';
import { TranslateModule } from '@ngx-translate/core';
import { HostListComponent } from 'app/views/tabs/hosts/hosts-list.component';

/**
 * Represents a form for creating or editing a chassis.
 */
@Component({
   templateUrl: './create-edit.component.html',
   styleUrls: ['./create-edit.component.scss'],
   standalone: true,
   imports: [
      ClarityModule,
      ReactiveFormsModule,
      TranslateModule,
      AlertComponent,
      HostListComponent,
   ],
})
export class CreateEditComponent implements OnInit {

   private static readonly EDIT_ACTION = 'edit';
   public chassis: Chassis;
   public action: string;
   public error?: Error;
   public form!: FormGroup;

   get name() {
      return this.form.get('name');
   }

   constructor(
      private readonly chassisService: ChassisService,
      private readonly globalService: GlobalService,
      route: ActivatedRoute,
   ) {
      this.action = route.snapshot.url[0].path;
      this.chassis = Chassis.empty();
   }

   ngOnInit(): void {
      if (this.isEditAction()) {
         Object.assign(this.chassis,
               this.globalService.htmlClientSdk.app.getContextObjects()[0]);
      }

      this.form = new FormGroup({
         name: new FormControl(this.chassis.name, [Validators.required]),
         serverType: new FormControl(this.chassis.serverType),
         dimensions: new FormControl(this.chassis.dimensions),
         isActive: new FormControl(this.chassis.isActive)
      });
   }

   onSubmit(): void {
      Object.assign(this.chassis, this.form.value);
      if (this.isEditAction()) {
         this.edit();
      } else {
         this.create();
      }
   }

   onCancel(): void {
      this.globalService.htmlClientSdk.modal.close();
   }

   isEditAction(): boolean {
      return this.action === CreateEditComponent.EDIT_ACTION;
   }

   create(): void {
      this.chassisService.create(this.chassis).subscribe({
         error: () => this.onCancel(),
         complete: () => this.onCancel()
      });
   }

   edit(): void {
      this.chassisService.edit(this.chassis).subscribe({
         error: () => this.onCancel(),
         complete: () => this.onCancel()
      });
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
}
