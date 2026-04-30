import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { RegularisationsResponse } from './regularisation.models';

@Injectable({ providedIn: 'root' })
export class RegularisationService {
  private readonly http = inject(HttpClient);

  getMyRegularisations(): Observable<RegularisationsResponse> {
    return this.http.get<RegularisationsResponse>(`${environment.apiBaseUrl}/me/regularisations`);
  }
}
