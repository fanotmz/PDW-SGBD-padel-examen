import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { LoginPageComponent } from './features/auth/login/login-page.component';
import { RegisterPageComponent } from './features/auth/register/register-page.component';
import { AdminPageComponent } from './features/admin/admin-page.component';
import { HomePageComponent } from './features/home/home-page.component';
import { CreateMatchPageComponent } from './features/matches/create-match/create-match-page.component';
import { MatchDetailPageComponent } from './features/matches/match-detail/match-detail-page.component';
import { MyMatchesPageComponent } from './features/matches/my-matches/my-matches-page.component';
import { OrganizedMatchesPageComponent } from './features/matches/organized-matches/organized-matches-page.component';
import { PublicMatchesPageComponent } from './features/matches/public-matches/public-matches-page.component';
import { EspacePrivePageComponent } from './features/player/espace-prive/espace-prive-page.component';
import { MainLayoutComponent } from './layout/main-layout.component';

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      { path: '', component: HomePageComponent },
      { path: 'login', component: LoginPageComponent },
      { path: 'inscription', component: RegisterPageComponent },
      { path: 'espace-prive', component: EspacePrivePageComponent, canActivate: [authGuard] },
      { path: 'matchs/creer', component: CreateMatchPageComponent, canActivate: [authGuard] },
      { path: 'matchs/:id', component: MatchDetailPageComponent, canActivate: [authGuard] },
      { path: 'me/matchs', component: MyMatchesPageComponent, canActivate: [authGuard] },
      { path: 'me/matchs/organises', component: OrganizedMatchesPageComponent, canActivate: [authGuard] },
      { path: 'matchs', component: PublicMatchesPageComponent, canActivate: [authGuard] },
      { path: 'admin', component: AdminPageComponent, canActivate: [authGuard] }
    ]
  },
  { path: '**', redirectTo: '' }
];
