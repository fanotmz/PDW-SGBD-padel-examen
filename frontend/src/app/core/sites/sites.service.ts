import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SiteOption } from './site.models';

@Injectable({ providedIn: 'root' })
export class SitesService {
  private readonly http = inject(HttpClient);

  getSites(): Observable<SiteOption[]> {
    return this.http.get<SiteOption[]>(`${environment.apiBaseUrl}/sites`);
  }
}
