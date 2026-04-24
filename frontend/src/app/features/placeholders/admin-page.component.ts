import { Component } from '@angular/core';

@Component({
  selector: 'app-admin-page',
  standalone: true,
  template: `
    <section class="placeholder-page">
      <p class="eyebrow">Navigation</p>
      <h1>Administration</h1>
      <p>
        Cette page est un placeholder pour la future zone d'administration. L'affichage précis par rôle
        sera finalisé lorsque le backend exposera clairement cette information.
      </p>
    </section>
  `,
  styles: `
    .placeholder-page {
      width: min(100%, 38rem);
      margin: 4rem auto;
      padding: 2rem;
      border: 1px solid #d7e0eb;
      border-radius: 1rem;
      background: #ffffff;
      box-shadow: 0 20px 45px rgba(15, 23, 42, 0.08);
    }

    .eyebrow {
      margin: 0 0 0.5rem;
      color: #0f766e;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.08em;
    }

    h1 {
      margin: 0 0 1rem;
      font-size: 2rem;
      color: #10233f;
    }

    p {
      margin: 0;
      color: #526277;
    }
  `
})
export class AdminPageComponent {}
