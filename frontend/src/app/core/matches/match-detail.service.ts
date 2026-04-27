import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MatchDetail } from './match-detail.models';

@Injectable({ providedIn: 'root' })
export class MatchDetailService {
  private readonly http = inject(HttpClient);

  getMatchDetail(id: number): Observable<MatchDetail> {
    return this.http.get<MatchDetail>(`${environment.apiBaseUrl}/matchs/${id}`);
  }
}
