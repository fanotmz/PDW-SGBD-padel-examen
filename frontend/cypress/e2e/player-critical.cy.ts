describe('Parcours joueur critique', () => {
  beforeEach(() => {
    cy.loginAsPlayer();
  });

  it('consulte les matchs publics puis ouvre le détail d un match', () => {
    cy.intercept('GET', '**/api/v1/matchs/public*').as('publicMatches');
    cy.intercept('GET', /\/api\/v1\/matchs\/\d+(?:\?.*)?$/).as('matchDetail');

    cy.visit('/matchs');
    cy.wait('@publicMatches').its('response.statusCode').should('eq', 200);

    cy.contains('h1', 'Matchs publics').should('be.visible');
    cy.contains('Site Nord').should('be.visible');
    cy.contains(/Nord T[12]/).should('be.visible');

    cy.contains('a', 'Voir le détail').first().click();

    cy.location('pathname').should('match', /^\/matchs\/\d+$/);
    cy.wait('@matchDetail').its('response.statusCode').should('eq', 200);
    cy.contains('h1', 'Détail du match').should('be.visible');
    cy.contains('h2', 'Informations générales').should('be.visible');
    cy.contains('Site').should('be.visible');
    cy.contains('Terrain').should('be.visible');
  });
});
