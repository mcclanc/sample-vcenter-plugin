/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

@Injectable({ providedIn: 'root' })
export class ResourceService {

   private readonly strings = [
      'shared.modal.createChassis',
      'shared.modal.editChassis',
      'shared.modal.deleteChassis',
      'ok',
      'cancel',
      'actions.delete.content',
      'errors.vcenterConnectivity',
      'errors.duplicateChassisName',
      'errors.general',
      'errors.websocketSessionTicket',
   ] as const satisfies string[];
   private localizedStrings?: Record<typeof this.strings[number], string>;

   constructor(private translate: TranslateService) {
   }

   public getString(str: typeof this.strings[number]): string {
      return this.localizedStrings && Object.hasOwn(this.localizedStrings, str)
         ? this.localizedStrings[str]
         : str;
   }

   public loadStrings() {
      this.translate.get(this.strings)
         .subscribe((result: typeof this.localizedStrings) => {
            this.localizedStrings = result;
         });
   }
}
