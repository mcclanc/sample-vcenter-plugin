/*
 * ******************************************************************
 * Copyright (c) 2018-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

import { Component, OnInit } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { GlobalService } from '~services/global.service';
import { Router, RouterOutlet } from "@angular/router";
import { ResourceService } from '~services/resource.service';
import { MessagingService } from '~services/messaging.service';
import { Theme, ThemeService } from '~services/theme.service';

@Component({
   selector: "app-root",
   templateUrl: "./app.component.html",
   styleUrls: ["./app.component.scss"],
   standalone: true,
   imports: [RouterOutlet],
})
export class AppComponent implements OnInit {
   private initialThemeLoadComplete = false;
   private messagingServiceInitialized = false;

   public get initialized(): boolean {
      return (
         this.globalService.htmlClientSdk.isInitialized()
            && this.initialThemeLoadComplete
            && this.messagingServiceInitialized
      );
   }

   constructor(
      private readonly translate: TranslateService,
      private readonly globalService: GlobalService,
      private readonly messagingService: MessagingService,
      private readonly router: Router,
      private readonly resourceService: ResourceService,
      private readonly themeService: ThemeService,
   ) {}

   ngOnInit(): void {
      this.translate.onLangChange.subscribe(() => {
         this.resourceService.loadStrings();
      });
      this.translate.addLangs(["en-US", "de-DE", "fr-FR"]);
      this.translate.setDefaultLang("en-US");
      this.globalService.htmlClientSdk.initialize(() => {
         this.messagingService.init$().subscribe({
            error: () => {
               this.messagingServiceInitialized = true;
            },
            complete: () => {
               this.messagingServiceInitialized = true;
            },
         });

         const navData = this.globalService.htmlClientSdk.app.getNavigationData();
         if (navData && navData.navigationPath && navData.selectedChassis) {
            this.router.navigateByUrl(navData.navigationPath, {
               state: { selectedChassis: navData.selectedChassis },
               replaceUrl: true,
            });
         }
         const locale = this.globalService.htmlClientSdk.app.getClientLocale();
         if (locale && this.translate.getLangs().includes(locale)) {
            this.translate.use(locale);
         } else {
            this.translate.use(this.translate.getDefaultLang());
         }

         if (
            this.globalService.htmlClientSdk.app.getTheme
            && this.globalService.htmlClientSdk.event.onThemeChanged
         ) {
            this.loadTheme(this.globalService.htmlClientSdk.app.getTheme());
            this.globalService.htmlClientSdk.event.onThemeChanged(
               this.loadTheme.bind(this)
            );
         } else {
            this.loadTheme({ name: "light" });
         }
      });
   }

   private async loadTheme(theme: { name: Theme }) {
      await this.themeService.setTheme(theme?.name || 'light');

      if (!this.initialThemeLoadComplete) {
         this.initialThemeLoadComplete = true;
      }
   }
}
