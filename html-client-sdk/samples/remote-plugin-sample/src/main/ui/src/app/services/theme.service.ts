/*
 * ******************************************************************
 * Copyright (c) 2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

import { DOCUMENT } from '@angular/common';
import { Inject, Injectable } from '@angular/core';
import { firstValueFrom, fromEvent } from 'rxjs';

export type Theme = 'light' | 'dark';

interface LinkData {
   /**
    * Id of the link element.
    */
   id: string;
   /**
    * Cached link element to prevent retrieving it from the document on every theme switch.
    */
   element?: HTMLLinkElement;
}

@Injectable({
   providedIn: 'root',
})
export class ThemeService {
   private readonly supportedThemeNames = new Set<Theme>(['light', 'dark']);
   private readonly cache: Record<Theme, LinkData> = {
      light: {
         id: 'theme-stylesheet-light'
      },
      dark: {
         id: 'theme-stylesheet-dark'
      },
   };

   constructor(@Inject(DOCUMENT) private readonly document: Document) {}

   /**
    * Switch the theme CSS.
    */
   public async setTheme(theme: Theme) {
      if (!this.supportedThemeNames.has(theme)) {
         theme = 'light';
      }

      // Enable the new theme.
      await this.setLinkElementEnabledState(this.getLinkElementForTheme(theme), true);

      this.document.documentElement.dataset.theme = theme;
      this.document.body.setAttribute('cds-theme', theme);

      const nonActiveLinkElement = this.getLinkElementForTheme(theme === 'dark' ? 'light' : 'dark');

      // Disable the previous theme after the new theme is enabled and all necessary body attributes are changed.
      // This drastically reduces UI flickering if the style sheets aren't loaded yet.
      await this.setLinkElementEnabledState(nonActiveLinkElement, false);
   }

   private async setLinkElementEnabledState(
      link: HTMLLinkElement | undefined,
      enabled: boolean
   ): Promise<void> {
      if (!link) {
         return;
      }

      let onLoadPromise = undefined;

      if (enabled) {
         // Only wait for style sheets that haven't loaded yet. Checking the sheet property is the closest API to
         // checking whether the style sheet is loaded.
         if (!link.sheet) {
            onLoadPromise = firstValueFrom(fromEvent(link, 'load'));
         }

         if (link.relList.replace('prefetch', 'stylesheet')) {
            link.as = '';
         }
      }

      if (link.sheet) {
         link.sheet.disabled = !enabled;
      }

      if (onLoadPromise) {
         // Wait for the style to load if necessary.
         await onLoadPromise;
      }
   }

   private getLinkElementForTheme(theme: Theme): HTMLLinkElement | undefined {
      const link = this.cache[theme];
      if (!link.element || !this.document.contains(link.element)) {
         link.element = (this.document.getElementById(link.id) as HTMLLinkElement) ?? undefined;
      }

      return link.element;
   }
}
