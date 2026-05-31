import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PlayerMatchSummary } from './player-match-summary.models';

@Injectable({ providedIn: 'root' })
export class PlayerMatchesService {
  private readonly http = inject(HttpClient);

  getMyMatches(): Observable<PlayerMatchSummary[]> {
    return this.http.get<PlayerMatchSummary[]>(`${environment.apiBaseUrl}/me/matchs`);
  }
}
