/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ClarityModule } from '@clr/angular';
import { TranslateModule } from '@ngx-translate/core';
import { GlobalService } from '~services/global.service';

@Component({
   templateUrl: './vm-action-modal.component.html',
   styleUrl: './vm-action-modal.component.scss',
   standalone: true,
   imports: [FormsModule, ClarityModule, TranslateModule],
})
export class VmActionModalComponent {
   constructor(private globalService: GlobalService) {
   }

   onClose(): void {
      this.closeModal();
   }

   onSubmit(): void {
      this.closeModal();
   }

   private closeModal(): void {
      this.globalService.htmlClientSdk.modal.close();
   }
}
