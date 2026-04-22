/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */
import {
   HttpErrorResponse,
   HttpEvent,
   HttpHandler, HttpHeaders,
   HttpInterceptor,
   HttpRequest
} from '@angular/common/http';
import { catchError, map, mergeMap } from 'rxjs/operators';
import {
   Observable,
   ObservableInput,
   throwError
} from 'rxjs';
import { ResourceService } from '~services/resource.service';
import { Injectable } from '@angular/core';
import { GlobalService } from '~services/global.service';

export interface SessionInfo {
   sessionToken: string;
}

export interface PluginBackendInfo {
   allPluginBackendServers: Array<PluginBackendServerInfo>;
   backendServersPerVc: { [vcGuid: string]: Array<PluginBackendServerInfo> };
}

export interface PluginBackendServerInfo {
   proxiedBaseUrl: string;
   type: string;
}

/**
 * Intercepts error responses and locales the message if possible.
 */
@Injectable({ providedIn: 'root' })
export class RemotePluginHttpInterceptor implements HttpInterceptor {

   constructor(private globalService: GlobalService,
         private resourceService: ResourceService) {
   }

   intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
      if (request.url && request.url.startsWith('./')) {
         return next.handle(request);
      }
      return this.getHttpHeaders().pipe(mergeMap((httpHeaders: HttpHeaders) =>
         this.getPluginBackends().pipe(mergeMap((backendInfo: PluginBackendInfo) => {
            if (!backendInfo.allPluginBackendServers.length) {
               return throwError(new Error(this.resourceService.getString('errors.general')));
            }
            const chassisSampleServerProxiedBaseUrl: string =
                  backendInfo.allPluginBackendServers[0].proxiedBaseUrl;
            const url =
                  `${window.location.origin}${chassisSampleServerProxiedBaseUrl}/sample-ui/rest/${request.url}`;
            const interceptedRequest: HttpRequest<unknown> = request.clone({
               headers: httpHeaders,
               url
            });
            return next.handle(interceptedRequest).pipe(catchError(
                  (response) => this.catchResponseError(response)));
         })))) as Observable<HttpEvent<unknown>>;
   }

   protected getPluginBackends(): Observable<PluginBackendInfo> {
      return new Observable(observer => {
         this.globalService.htmlClientSdk.app.getPluginBackendInfo((backendInfo: PluginBackendInfo) => {
            if (backendInfo) {
               observer.next(backendInfo);
               observer.complete();
            } else {
               observer.error('Error retrieving plugin backends information.');
            }
         });
      });
   }

   private catchResponseError(response: HttpErrorResponse): ObservableInput<unknown> {
      if (!!response.error && !!response.error.localeKey) {
         response.error.message =
               this.resourceService.getString(response.error.localeKey);
         return throwError(() => response.error);
      }
      return throwError(() => response);
   }

   private getHttpHeaders(): Observable<HttpHeaders> {
      return this.getSessionInfo().pipe(map((sessionInfo: SessionInfo) => new HttpHeaders({
            'Content-Type': 'application/json;charset=utf-8',
            Accept: 'application/json;charset=utf-8',
            'Cache-Control': 'no-cache',
            Pragma: 'no-cache',
            Expires: 'Sat, 01 Jan 2000 00:00:00 GMT',
            'vmware-api-session-id': sessionInfo.sessionToken
         })));
   }

   private getSessionInfo(): Observable<SessionInfo> {
      return new Observable(observer => {
         this.globalService.htmlClientSdk.app.getSessionInfo((sessionInfo: SessionInfo) => {
            observer.next(sessionInfo);
            observer.complete();
         });
      });
   }
}
