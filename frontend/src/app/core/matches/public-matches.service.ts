import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PublicMatch, PublicMatchesFilters } from './public-match.models';

@Injectable({ providedIn: 'root' })
export class PublicMatchesService {
  private readonly http = inject(HttpClient);

  getPublicMatches(filters: PublicMatchesFilters = {}): Observable<PublicMatch[]> {
    let params = new HttpParams();

    if (filters.from) {
      params = params.set('from', filters.from);
    }

    if (filters.to) {
      params = params.set('to', filters.to);
    }

    if (filters.siteId != null) {
      params = params.set('siteId', filters.siteId);
    }

    return this.http.get<PublicMatch[]>(`${environment.apiBaseUrl}/matchs/public`, { params });
  }
}
