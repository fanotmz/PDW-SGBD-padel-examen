import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AddPrivatePlayerToMatchRequest, MatchParticipation } from './match-participation.models';

@Injectable({ providedIn: 'root' })
export class MatchParticipationService {
  private readonly http = inject(HttpClient);

  rejoindreMatchPublic(matchId: number): Observable<MatchParticipation> {
    return this.http.post<MatchParticipation>(
      `${environment.apiBaseUrl}/matchs/${matchId}/participants/public`,
      {}
    );
  }

  ajouterJoueurPrive(
    matchId: number,
    joueurMatriculeAAjouter: string
  ): Observable<MatchParticipation> {
    const body: AddPrivatePlayerToMatchRequest = { joueurMatriculeAAjouter };

    return this.http.post<MatchParticipation>(
      `${environment.apiBaseUrl}/matchs/${matchId}/participants/prive`,
      body
    );
  }
}
