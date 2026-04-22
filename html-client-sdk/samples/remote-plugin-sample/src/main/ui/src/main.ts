/*
 * ******************************************************************
 * Copyright (c) 2023-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

/* eslint-disable import/no-internal-modules */

import {
   enableProdMode,
   importProvidersFrom,
   provideZoneChangeDetection,
} from "@angular/core";
import { environment } from './environments/environment';
import { bootstrapApplication } from "@angular/platform-browser";
import { AppComponent } from "app/app.component";
import {
   HTTP_INTERCEPTORS,
   HttpClient,
   provideHttpClient,
   withInterceptorsFromDi,
} from "@angular/common/http";
import { RemotePluginHttpInterceptor } from "~services/http.interceptor";
import { TranslateLoader, TranslateModule } from "@ngx-translate/core";
import { routes } from "app/app.routes";
import { provideRouter, withHashLocation } from "@angular/router";
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import {
   chartCollectionIcons,
   ClarityIcons,
   commerceCollectionIcons,
   coreCollectionIcons,
   essentialCollectionIcons,
   mediaCollectionIcons,
   socialCollectionIcons,
   technologyCollectionIcons,
   textEditCollectionIcons,
   travelCollectionIcons,
} from '@cds/core/icon';

import '@cds/core/icon/register.js';

if (environment.production) {
   console.log(environment);
   enableProdMode();
}

function createTranslateLoader(http: HttpClient) {
   return new TranslateHttpLoader(http, "./assets/i18n/", ".json");
}

ClarityIcons.addIcons(
   ...coreCollectionIcons,
   ...essentialCollectionIcons,
   ...commerceCollectionIcons,
   ...mediaCollectionIcons,
   ...socialCollectionIcons,
   ...travelCollectionIcons,
   ...textEditCollectionIcons,
   ...technologyCollectionIcons,
   ...chartCollectionIcons,
   [
      "chassisIcon",
      `<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16'><title>chassis</title><g id='083e8a78-1df0-436f-9a22-5827915fd5f3' data-name='Layer 1'><path d='M.53,9H14.47a.53.53,0,0,1,.53.53v2.93a.53.53,0,0,1-.53.53H.54A.54.54,0,0,1,0,12.47V9.54A.53.53,0,0,1,.53,9Z' fill='#cbd9e0'/><path d='M15,9,13.17,4.07c-.15-.29-.22-.51-.77-.51H3.6c-.54,0-.62.23-.77.52L1,9Z' fill='#cad8df'/><path d='M.54,9H14.47a.53.53,0,0,1,.53.53v2.93a.53.53,0,0,1-.53.53H.54A.54.54,0,0,1,0,12.47V9.55A.54.54,0,0,1,.54,9Z' fill='#9bb2ba'/><path d='M15.44,9H.66a1,1,0,0,1,1-1H14.48A1,1,0,0,1,15.44,9Z' fill='#fff'/><path d='M15,14H1a1,1,0,0,1-1-1V9.26L2,3.77A1.2,1.2,0,0,1,3.23,3h9.39a1.2,1.2,0,0,1,1.23.77l.06.12L16,9.56V13A1,1,0,0,1,15,14ZM3.26,4C3,4,3,4,2.93,4.22L1,9.69V13l14,0,0-3.42v0L13,4.23C12.89,4,12.89,4,12.66,4Z' fill='#235d80'/><rect x='2' y='10' width='1' height='2' fill='#235d80'/><rect x='4' y='10.04' width='0.91' height='1.93' fill='#235d80'/><rect x='6' y='10' width='1' height='2' fill='#235d80'/><rect x='8.02' y='10' width='3.98' height='1' fill='#235d80'/><rect x='13' y='10' width='1' height='1' fill='#235d80'/></g></svg>`,
   ]
);

bootstrapApplication(AppComponent, {
   providers: [
      provideZoneChangeDetection({ eventCoalescing: true }),
      provideRouter(routes, withHashLocation()),
      {
         provide: HTTP_INTERCEPTORS,
         useClass: RemotePluginHttpInterceptor,
         multi: true,
      },
      provideHttpClient(withInterceptorsFromDi()),
      importProvidersFrom(
         TranslateModule.forRoot({
            loader: {
               provide: TranslateLoader,
               useFactory: createTranslateLoader,
               deps: [HttpClient],
            },
         })
      ),
   ],
}).catch((err) => console.error(err));;
