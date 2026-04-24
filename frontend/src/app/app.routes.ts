import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { LoginPageComponent } from './features/auth/login/login-page.component';
import { HomePageComponent } from './features/home/home-page.component';
import { AdminPageComponent } from './features/placeholders/admin-page.component';
import { MatchsPageComponent } from './features/placeholders/matchs-page.component';
import { EspacePrivePageComponent } from './features/test/espace-prive-page.component';
import { MainLayoutComponent } from './layout/main-layout.component';

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      { path: '', component: HomePageComponent },
      { path: 'login', component: LoginPageComponent },
      { path: 'espace-prive', component: EspacePrivePageComponent, canActivate: [authGuard] },
      { path: 'matchs', component: MatchsPageComponent, canActivate: [authGuard] },
      { path: 'admin', component: AdminPageComponent, canActivate: [authGuard] }
    ]
  },
  { path: '**', redirectTo: '' }
];
