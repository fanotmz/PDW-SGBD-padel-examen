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

  payLateCancellationRegularisation(participationId: number, montant: number): Observable<void> {
    return this.http.post<void>(
      `${environment.apiBaseUrl}/me/regularisations/${participationId}/paiement`,
      { montant }
    );
  }
}
