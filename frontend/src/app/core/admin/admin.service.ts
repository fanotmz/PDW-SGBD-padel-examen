import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AdminInfoResponse,
  PendingRegistrationItem,
  RegistrationDecisionResponse,
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
