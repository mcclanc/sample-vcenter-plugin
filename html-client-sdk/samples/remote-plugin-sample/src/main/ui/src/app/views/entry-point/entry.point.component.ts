/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

import { Component } from "@angular/core";
import { ActivatedRoute, RouterLink, RouterOutlet } from '@angular/router';
import { ClarityModule } from '@clr/angular';
import { TranslateModule } from "@ngx-translate/core";

interface NavItem {
   route: string,
   iconClass: string,
   labelKey: string,
}

@Component({
   templateUrl: "./entry.point.component.html",
   styleUrls: ["./entry.point.component.scss"],
   standalone: true,
   imports: [
      RouterLink,
      RouterOutlet,
      ClarityModule,
      TranslateModule,
   ],
})
export class EntryPointComponent {
   readonly navItems: NavItem[] = [
      {
         route: 'welcome',
         iconClass: 'welcome-icon',
         labelKey: 'welcome.title',
      },
      {
         route: 'settings',
         iconClass: 'settings-icon',
         labelKey: 'common.settings',
      },
      {
         route: 'list',
         iconClass: 'chassis-list-icon',
         labelKey: 'list.chassisList',
      }
   ];

   constructor(private readonly route: ActivatedRoute) {}

   isNavItemActive(item: NavItem): boolean {
      return this.route.snapshot.children?.[0]?.routeConfig?.path === item.route;
   }
}
