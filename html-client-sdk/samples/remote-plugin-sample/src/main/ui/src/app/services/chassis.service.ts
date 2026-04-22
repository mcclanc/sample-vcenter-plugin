/*
 * ******************************************************************
 * Copyright (c) 2018-2025 Broadcom. All Rights Reserved.
 * Broadcom Confidential. The term "Broadcom" refers to Broadcom Inc.
 * and/or its subsidiaries.
 * ******************************************************************
 */

import { Injectable } from '@angular/core';
import { Chassis } from '~models/chassis.model';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class ChassisService {

   constructor(private http: HttpClient) {
   }

   /**
    * Creates a new object of type Chassis.
    *
    * @param chassis - the new chassis to be created.
    */
   public create(chassis: Chassis): Observable<void> {
      chassis.name = chassis.name.trim();
      return this.http.post('chassis', JSON.stringify(chassis))
            .pipe(map(() => undefined)) as Observable<void>;
   }

   /**
    * Edit the given chassis.
    *
    * @param chassis - the edited chassis.
    */
   public edit(chassis: Chassis): Observable<void> {
      const newChassis = Chassis.clone(chassis);
      newChassis.name = newChassis.name.trim();
      return this.http.put('chassis/edit', JSON.stringify(chassis))
            .pipe(map(() => undefined)) as Observable<void>;
   }

   public remove(target: string | string[]): Observable<void> {
      if (typeof target === 'string') {
         return this.http.delete(`chassis/${target}`)
            .pipe(map(() => undefined)) as Observable<void>;
      } else {
         const chassisIds: string = target.join(',');
         return this.http.delete('chassis/delete', {
            params: {ids: `${chassisIds}`}
         }).pipe(map(() => undefined)) as Observable<void>;
      }
   }

   /**
    * Retrieves all related Chassis to the provided objectId.
    *
    * @param objectId
    */
   public getRelatedChassis(objectId: string): Observable<Chassis[]> {
      return this.http.get<Chassis[]>(`hosts/${objectId}/chassis`);
   }

   /**
    * Retrieves all chassis.
    */
   public getAllChassis(): Observable<Chassis[]> {
      return this.http.get<Chassis[]>('chassis').pipe(
            map((result: Chassis[]) => {
               for (const chassis of result) {
                  chassis.healthStatus = 45;
                  chassis.complianceStatus = 81;
               }
               return result;
            }));
   }
}
