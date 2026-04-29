import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Paiement, PayParticipationRequest } from './paiement.models';

@Injectable({ providedIn: 'root' })
export class PaiementService {
  private readonly http = inject(HttpClient);

  payerParticipation(participationId: number, montant: number): Observable<Paiement> {
    const body: PayParticipationRequest = { montant };

    return this.http.post<Paiement>(
      `${environment.apiBaseUrl}/participations/${participationId}/paiements`,
      body
    );
  }
}
