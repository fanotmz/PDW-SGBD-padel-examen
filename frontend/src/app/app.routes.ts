import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { MainLayoutComponent } from './layout/main-layout.component';

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/home/home-page.component').then((m) => m.HomePageComponent)
      },
      {
        path: 'login',
        loadComponent: () =>
          import('./features/auth/login/login-page.component').then((m) => m.LoginPageComponent)
      },
      {
        path: 'inscription',
        loadComponent: () =>
          import('./features/auth/register/register-page.component').then((m) => m.RegisterPageComponent)
      },
      {
        path: 'espace-prive',
        loadComponent: () =>
          import('./features/player/espace-prive/espace-prive-page.component').then(
            (m) => m.EspacePrivePageComponent
          ),
        canActivate: [authGuard]
      },
      {
        path: 'matchs/creer',
        loadComponent: () =>
          import('./features/matches/create-match/create-match-page.component').then(
            (m) => m.CreateMatchPageComponent
          ),
        canActivate: [authGuard]
      },
      {
        path: 'matchs/:id',
        loadComponent: () =>
          import('./features/matches/match-detail/match-detail-page.component').then(
            (m) => m.MatchDetailPageComponent
          ),
        canActivate: [authGuard]
      },
      {
        path: 'me/matchs',
        loadComponent: () =>
          import('./features/matches/my-matches/my-matches-page.component').then(
            (m) => m.MyMatchesPageComponent
          ),
        canActivate: [authGuard]
      },
      {
        path: 'me/matchs/organises',
        loadComponent: () =>
          import('./features/matches/organized-matches/organized-matches-page.component').then(
            (m) => m.OrganizedMatchesPageComponent
          ),
        canActivate: [authGuard]
      },
      {
        path: 'matchs',
        loadComponent: () =>
          import('./features/matches/public-matches/public-matches-page.component').then(
            (m) => m.PublicMatchesPageComponent
          ),
        canActivate: [authGuard]
      },
      {
        path: 'admin',
        loadComponent: () =>
          import('./features/admin/admin-page.component').then((m) => m.AdminPageComponent),
        canActivate: [authGuard]
      },
      {
        path: 'admin/fermetures',
        loadComponent: () =>
          import('./features/admin/fermetures/admin-closures-page.component').then(
            (m) => m.AdminClosuresPageComponent
          ),
        canActivate: [authGuard]
      },
      {
        path: 'admin/horaires',
        loadComponent: () =>
          import('./features/admin/horaires/admin-schedules-page.component').then(
            (m) => m.AdminSchedulesPageComponent
          ),
        canActivate: [authGuard]
      },
      {
        path: 'admin/sites',
        loadComponent: () =>
          import('./features/admin/sites/admin-sites-page.component').then(
            (m) => m.AdminSitesPageComponent
          ),
        canActivate: [authGuard]
      },
      {
        path: 'admin/sites/:siteId/matchs',
        loadComponent: () =>
          import('./features/admin/site-matches/admin-site-matches-page.component').then(
            (m) => m.AdminSiteMatchesPageComponent
          ),
        canActivate: [authGuard]
      },
      {
        path: 'admin/statistiques',
        loadComponent: () =>
          import('./features/admin/statistiques/admin-statistics-page.component').then(
            (m) => m.AdminStatisticsPageComponent
          ),
        canActivate: [authGuard]
      },
      {
        path: 'admin/joueurs',
        loadComponent: () =>
          import('./features/admin/joueurs/admin-players-page.component').then(
            (m) => m.AdminPlayersPageComponent
          ),
        canActivate: [authGuard]
      },
      {
        path: 'admin/inscriptions',
        loadComponent: () =>
          import('./features/admin/inscriptions/admin-registrations-page.component').then(
            (m) => m.AdminRegistrationsPageComponent
          ),
        canActivate: [authGuard]
      }
    ]
  },
  { path: '**', redirectTo: '' }
];
