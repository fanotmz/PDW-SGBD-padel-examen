describe('Parcours administrateur critique', () => {
  beforeEach(() => {
    cy.clearLocalStorage();
  });

  it('connecte un administrateur via l interface et affiche le dashboard admin', () => {
    cy.intercept('POST', '**/api/v1/auth/login').as('login');
    cy.intercept('GET', '**/api/v1/admin/info').as('adminInfo');

    cy.visit('/login');
    cy.get('input[autocomplete="username"]').should('be.visible').type('admin.global.dev');
    cy.get('input[autocomplete="current-password"]').should('be.visible').type('admin123', {
      log: false
    });
    cy.contains('button', 'Se connecter').click();

    cy.wait('@login').its('response.statusCode').should('eq', 200);
    cy.location('pathname').should('eq', '/admin');
    cy.wait('@adminInfo').its('response.statusCode').should('eq', 200);

    cy.contains('h1', 'Administration').should('be.visible');
    cy.contains('Statut API admin').should('be.visible');
    cy.contains('API admin opérationnelle').should('be.visible');
  });

  it('consulte l historique des matchs du site 1', () => {
    cy.loginAsAdmin();

    cy.intercept('GET', '**/api/v1/admin/sites/1/matchs*').as('adminSiteMatches');

    cy.visit('/admin/sites/1/matchs');
    cy.wait('@adminSiteMatches').its('response.statusCode').should('eq', 200);

    cy.contains('h1', 'Matchs du site').should('be.visible');
    cy.contains('Site Nord').should('be.visible');
    cy.contains(/Nord T[12]/).should('be.visible');
    cy.contains('a', 'Voir détail').should('be.visible');
  });
});
