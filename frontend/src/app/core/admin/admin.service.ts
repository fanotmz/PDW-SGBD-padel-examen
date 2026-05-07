import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AdminCaStatsResponse,
  AdminDettesStatsResponse,
  AdminGlobalClosureResponse,
  AdminPlayerResponse,
  AdminSitePlayerResponse,
  AdminSiteClosureResponse,
  AdminSiteScheduleResponse,
  AdminMatchsStatsResponse,
  AdminInfoResponse,
  PendingRegistrationItem,
  RegistrationDecisionResponse,
  CreateGlobalClosureRequest,
  CreateSiteDateClosureRequest,
  CreateSitePeriodClosureRequest,
  UpdateSiteDateClosureRequest,
  UpdateSitePeriodClosureRequest,
  UpsertSiteScheduleRequest,
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

  getSiteSchedules(siteId: number): Observable<AdminSiteScheduleResponse[]> {
    return this.http.get<AdminSiteScheduleResponse[]>(
      `${environment.apiBaseUrl}/admin/sites/${siteId}/horaires`
    );
  }

  getGlobalCaStats(from: string, to: string): Observable<AdminCaStatsResponse> {
    return this.http.get<AdminCaStatsResponse>(`${environment.apiBaseUrl}/admin/stats/ca`, {
      params: { from, to }
    });
  }

  getGlobalMatchsStats(from: string, to: string): Observable<AdminMatchsStatsResponse> {
    return this.http.get<AdminMatchsStatsResponse>(`${environment.apiBaseUrl}/admin/stats/matchs`, {
      params: { from, to }
    });
  }

  getGlobalDettesStats(): Observable<AdminDettesStatsResponse> {
    return this.http.get<AdminDettesStatsResponse>(`${environment.apiBaseUrl}/admin/stats/dettes`);
  }

  getSiteCaStats(siteId: number, from: string, to: string): Observable<AdminCaStatsResponse> {
    return this.http.get<AdminCaStatsResponse>(
      `${environment.apiBaseUrl}/admin/sites/${siteId}/stats/ca`,
      { params: { from, to } }
    );
  }

  getSiteMatchsStats(
    siteId: number,
    from: string,
    to: string
  ): Observable<AdminMatchsStatsResponse> {
    return this.http.get<AdminMatchsStatsResponse>(
      `${environment.apiBaseUrl}/admin/sites/${siteId}/stats/matchs`,
      { params: { from, to } }
    );
  }

  getSiteDettesStats(siteId: number): Observable<AdminDettesStatsResponse> {
    return this.http.get<AdminDettesStatsResponse>(
      `${environment.apiBaseUrl}/admin/sites/${siteId}/stats/dettes`
    );
  }

  createSiteSchedule(
    siteId: number,
    payload: UpsertSiteScheduleRequest
  ): Observable<AdminSiteScheduleResponse> {
    return this.http.post<AdminSiteScheduleResponse>(
      `${environment.apiBaseUrl}/admin/sites/${siteId}/horaires`,
      payload
    );
  }

  updateSiteSchedule(
    siteId: number,
    scheduleId: number,
    payload: UpsertSiteScheduleRequest
  ): Observable<AdminSiteScheduleResponse> {
    return this.http.put<AdminSiteScheduleResponse>(
      `${environment.apiBaseUrl}/admin/sites/${siteId}/horaires/${scheduleId}`,
      payload
    );
  }

  deleteSiteSchedule(siteId: number, scheduleId: number): Observable<void> {
    return this.http.delete<void>(
      `${environment.apiBaseUrl}/admin/sites/${siteId}/horaires/${scheduleId}`
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

  updateSiteDateClosure(
    siteId: number,
    closureId: number,
    payload: UpdateSiteDateClosureRequest
  ): Observable<AdminSiteClosureResponse> {
    return this.http.put<AdminSiteClosureResponse>(
      `${environment.apiBaseUrl}/admin/sites/${siteId}/fermetures/${closureId}/date`,
      payload
    );
  }

  updateSitePeriodClosure(
    siteId: number,
    closureId: number,
    payload: UpdateSitePeriodClosureRequest
  ): Observable<AdminSiteClosureResponse> {
    return this.http.put<AdminSiteClosureResponse>(
      `${environment.apiBaseUrl}/admin/sites/${siteId}/fermetures/${closureId}/periode`,
      payload
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
