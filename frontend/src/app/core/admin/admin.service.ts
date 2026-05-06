import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AdminGlobalClosureResponse,
  AdminPlayerResponse,
  AdminSitePlayerResponse,
  AdminSiteClosureResponse,
  AdminInfoResponse,
  PendingRegistrationItem,
  RegistrationDecisionResponse,
  CreateGlobalClosureRequest,
  CreateSiteDateClosureRequest,
  CreateSitePeriodClosureRequest,
  ValidateRegistrationRequest
} from './admin.models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  getInfo(): Observable<AdminInfoResponse> {
    return this.http.get<AdminInfoResponse>(`${environment.apiBaseUrl}/admin/info`);
  }

  getPendingRegistrations(): Observable<PendingRegistrationItem[]> {
    return this.http.get<PendingRegistrationItem[]>(`${environment.apiBaseUrl}/admin/inscriptions`);
  }

  getPlayers(): Observable<AdminPlayerResponse[]> {
    return this.http.get<AdminPlayerResponse[]>(`${environment.apiBaseUrl}/joueurs`);
  }

  getPlayersBySite(siteId: number): Observable<AdminSitePlayerResponse[]> {
    return this.http.get<AdminSitePlayerResponse[]>(`${environment.apiBaseUrl}/admin/sites/${siteId}/joueurs`);
  }

  getGlobalClosures(): Observable<AdminGlobalClosureResponse[]> {
    return this.http.get<AdminGlobalClosureResponse[]>(`${environment.apiBaseUrl}/fermetures-globales`);
  }

  getSiteClosures(siteId: number): Observable<AdminSiteClosureResponse[]> {
    return this.http.get<AdminSiteClosureResponse[]>(
      `${environment.apiBaseUrl}/admin/sites/${siteId}/fermetures`
    );
  }

  createGlobalClosure(payload: CreateGlobalClosureRequest): Observable<AdminGlobalClosureResponse> {
    return this.http.post<AdminGlobalClosureResponse>(`${environment.apiBaseUrl}/fermetures-globales`, payload);
  }

  createSiteDateClosure(
    siteId: number,
    payload: CreateSiteDateClosureRequest
  ): Observable<AdminSiteClosureResponse> {
    return this.http.post<AdminSiteClosureResponse>(
      `${environment.apiBaseUrl}/admin/sites/${siteId}/fermetures/date`,
      payload
    );
  }

  createSitePeriodClosure(
    siteId: number,
    payload: CreateSitePeriodClosureRequest
  ): Observable<AdminSiteClosureResponse> {
    return this.http.post<AdminSiteClosureResponse>(
      `${environment.apiBaseUrl}/admin/sites/${siteId}/fermetures/periode`,
      payload
    );
  }

  deleteGlobalClosure(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiBaseUrl}/fermetures-globales/${id}`);
  }

  deleteSiteClosure(siteId: number, closureId: number): Observable<void> {
    return this.http.delete<void>(
      `${environment.apiBaseUrl}/admin/sites/${siteId}/fermetures/${closureId}`
    );
  }

  validateRegistration(
    userId: number,
    payload: ValidateRegistrationRequest
  ): Observable<RegistrationDecisionResponse> {
    return this.http.post<RegistrationDecisionResponse>(
      `${environment.apiBaseUrl}/admin/inscriptions/${userId}/validate`,
      payload
    );
  }

  rejectRegistration(userId: number): Observable<RegistrationDecisionResponse> {
    return this.http.post<RegistrationDecisionResponse>(
      `${environment.apiBaseUrl}/admin/inscriptions/${userId}/reject`,
      null
    );
  }
}
