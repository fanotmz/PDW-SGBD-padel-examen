import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AdminInfoResponse, PendingRegistrationItem } from './admin.models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  getInfo(): Observable<AdminInfoResponse> {
    return this.http.get<AdminInfoResponse>(`${environment.apiBaseUrl}/admin/info`);
  }

  getPendingRegistrations(): Observable<PendingRegistrationItem[]> {
    return this.http.get<PendingRegistrationItem[]>(`${environment.apiBaseUrl}/admin/inscriptions`);
  }
}
