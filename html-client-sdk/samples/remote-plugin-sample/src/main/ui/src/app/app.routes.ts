/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

import { ListComponent } from './views/list/list.component';
import { PageNotFoundComponent } from './page-not-found.component';
import { CreateEditComponent } from './views/actions/create-edit/create-edit.component';
import { CreateWizardComponent } from './views/actions/create-edit/create-wizard.component';
import { WelcomeComponent } from './views/welcome/welcome.component';
import { SettingsComponent } from './views/settings/settings.component';
import { VmActionModalComponent } from './views/actions/vm/vm-action-modal.component';
import { EntryPointComponent } from './views/entry-point/entry.point.component';
import { VmMonitorComponent } from './views/vm-views/vm-monitor.component';
import { VmConfigureComponent } from './views/vm-views/vm-configure.component';
import { VmCardComponent } from './views/vm-views/vm-card.component';
import { HostCardComponent } from './views/host-views/host-card.component';
import { HostMonitorComponent } from './views/host-views/host.monitor.component';
import { Routes } from '@angular/router';

export const routes: Routes = [
   { path: 'create', component: CreateEditComponent },
   { path: 'create-wizard', component: CreateWizardComponent },
   { path: 'edit', component: CreateEditComponent },
   { path: 'vm-action-modal', component: VmActionModalComponent },
   {
      path: 'entry-point',
      component: EntryPointComponent,
      children: [
         { path: '', redirectTo: 'welcome', pathMatch: 'full' },
         { path: 'welcome', component: WelcomeComponent },
         { path: 'settings', component: SettingsComponent },
         { path: 'list', component: ListComponent },
      ]
   },
   { path: 'vm-monitor', component: VmMonitorComponent },
   { path: 'vm-configure', component: VmConfigureComponent },
   { path: 'vm-card', component: VmCardComponent },
   { path: 'host-card', component: HostCardComponent },
   { path: 'host-monitor', component: HostMonitorComponent },
   { path: '**', pathMatch: 'full', component: PageNotFoundComponent }
];
