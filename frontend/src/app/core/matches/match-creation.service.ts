import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateMatchPayload, CreatedMatch } from './create-match.models';

@Injectable({ providedIn: 'root' })
export class MatchCreationService {
  private readonly http = inject(HttpClient);

  createMatch(payload: CreateMatchPayload): Observable<CreatedMatch> {
    return this.http.post<CreatedMatch>(`${environment.apiBaseUrl}/matchs`, payload);
  }
}
