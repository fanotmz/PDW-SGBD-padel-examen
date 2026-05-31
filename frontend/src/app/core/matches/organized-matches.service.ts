import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { OrganizedMatchSummary } from './organized-match-summary.models';

@Injectable({ providedIn: 'root' })
export class OrganizedMatchesService {
  private readonly http = inject(HttpClient);

  getMyOrganizedMatches(): Observable<OrganizedMatchSummary[]> {
    return this.http.get<OrganizedMatchSummary[]>(
      `${environment.apiBaseUrl}/me/matchs/organises`
    );
  }
}
