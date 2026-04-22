/*
 * ******************************************************************
 * Copyright (c) 2019-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

import { Injectable } from '@angular/core';
import { Chassis } from '~models/chassis.model';
import { Host } from '~models/host.model';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class HostsService {

   constructor(private http: HttpClient) {
   }

   /**
    * Sends a get message to get all connected hosts
    */
   public getConnectedHosts(chassis: Chassis): Observable<Host[]> {
      const endpoint = chassis ? `chassis/${chassis.id}/hosts` : 'hosts';
      return this.http.get<Host[]>(endpoint);
   }

   /**
    * Sends a message to edit the Host object
    */
   public edit(host: Host): Observable<void> {
      const endpoint = 'hosts';
      return this.http.put(endpoint, host).pipe(map(() => undefined)) as Observable<void>;
   }
}
