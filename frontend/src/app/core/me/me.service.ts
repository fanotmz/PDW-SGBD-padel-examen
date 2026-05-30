import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MeProfile, MeStats } from './me.models';

@Injectable({ providedIn: 'root' })
export class MeService {
  private readonly http = inject(HttpClient);

  getMe(): Observable<MeProfile> {
    return this.http.get<MeProfile>(`${environment.apiBaseUrl}/me`);
  }

  getMyStats(): Observable<MeStats> {
    return this.http.get<MeStats>(`${environment.apiBaseUrl}/me/stats`);
  }
}
