/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

import {
   Component,
   Input,
} from '@angular/core';
import { Chassis } from '~models/chassis.model';
import { ChassisService } from '~services/chassis.service';
import {
   ConfirmationModalConfig,
   ModalConfig,
   ModalConfigService
} from '~services/modal.service';
import { GlobalService } from '~services/global.service';
import { ClarityModule } from '@clr/angular';
import { TranslateModule } from '@ngx-translate/core';

@Component({
   selector: 'app-custom-header',
   templateUrl: './header.component.html',
   styleUrls: ['./header.component.scss'],
   standalone: true,
   imports: [ClarityModule, TranslateModule],
})
export class HeaderComponent {

   @Input()
   chassis!: Chassis;

   constructor(
      private readonly chassisService: ChassisService,
      private readonly modalService: ModalConfigService,
      private readonly globalService: GlobalService,
   ) {}

   onDelete(): void {
      const config: ConfirmationModalConfig = this.modalService
            .createDeleteConfig([this.chassis.name], () => {
               this.deleteChassis();
            });
      this.globalService.htmlClientSdk.modal.openConfirmationModal(config);
   }

   onEdit(): void {
      const config: ModalConfig = this.modalService.createEditConfig();
      config.contextObjects = [Chassis.clone(this.chassis)];
      this.globalService.htmlClientSdk.modal.open(config);
   }

   onActivate(): void {
      if (!this.chassis.isActive) {
         return;
      }

      const newChassis = Chassis.clone(this.chassis);
      newChassis.isActive = true;
      this.chassisService.edit(newChassis).subscribe();
   }

   private deleteChassis() {
      this.chassisService.remove(this.chassis.id).subscribe();
   }
}
