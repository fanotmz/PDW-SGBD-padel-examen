import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TerrainOption } from './terrain.models';

@Injectable({ providedIn: 'root' })
export class TerrainsService {
  private readonly http = inject(HttpClient);

  getTerrains(siteId?: number): Observable<TerrainOption[]> {
    let params = new HttpParams();

    if (siteId != null) {
      params = params.set('siteId', siteId);
    }

    return this.http.get<TerrainOption[]>(`${environment.apiBaseUrl}/terrains`, { params });
  }
}
