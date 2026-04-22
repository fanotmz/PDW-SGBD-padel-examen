import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { LoginPageComponent } from './features/auth/login/login-page.component';
import { HomePageComponent } from './features/home/home-page.component';
import { EspacePrivePageComponent } from './features/test/espace-prive-page.component';

export const routes: Routes = [
  { path: '', component: HomePageComponent },
  { path: 'login', component: LoginPageComponent },
  { path: 'espace-prive', component: EspacePrivePageComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: '' }
];
