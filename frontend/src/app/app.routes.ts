import { Routes } from '@angular/router';
import { LoginPageComponent } from './features/auth/login/login-page.component';
import { HomePageComponent } from './features/home/home-page.component';

export const routes: Routes = [
  { path: '', component: HomePageComponent },
  { path: 'login', component: LoginPageComponent },
  { path: '**', redirectTo: '' }
];
