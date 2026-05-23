describe('Authentification et protection des routes', () => {
  beforeEach(() => {
    cy.clearLocalStorage();
  });

  it('redirige un visiteur non authentifié vers la page de connexion', () => {
    cy.visit('/me/matchs');

    cy.location('pathname').should('eq', '/login');
    cy.location('search').should('contain', 'returnUrl=%2Fme%2Fmatchs');
    cy.contains('mat-card-title', 'Connexion').should('be.visible');
  });

  it('connecte un joueur via l interface et donne accès à ses matchs', () => {
    cy.intercept('POST', '**/api/v1/auth/login').as('login');

    cy.visit('/login');
    cy.get('input[autocomplete="username"]').should('be.visible').type('joueur.global.dev');
    cy.get('input[autocomplete="current-password"]').should('be.visible').type('joueur123', {
      log: false
    });
    cy.contains('button', 'Se connecter').click();

    cy.wait('@login').its('response.statusCode').should('eq', 200);
    cy.location('pathname').should('eq', '/espace-prive');

    cy.visit('/me/matchs');
    cy.location('pathname').should('eq', '/me/matchs');
    cy.contains('h1', 'Mes matchs').should('be.visible');
  });

  it('refuse l accès à l administration pour un joueur authentifié', () => {
    cy.loginAsPlayer();
    cy.intercept('GET', '**/api/v1/admin/info').as('adminInfoForbidden');

    cy.visit('/admin');

    cy.wait('@adminInfoForbidden').its('response.statusCode').should('eq', 403);
    cy.location('pathname').should('eq', '/admin');
    cy.contains('Accès administrateur refusé.').should('be.visible');
  });
});
