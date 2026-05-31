type TestUser = 'player' | 'admin';

const users: Record<TestUser, { username: string; password: string; landingPath: string }> = {
  player: {
    username: 'joueur.global.dev',
    password: 'joueur123',
    landingPath: '/espace-prive'
  },
  admin: {
    username: 'admin.global.dev',
    password: 'admin123',
    landingPath: '/admin'
  }
};

declare global {
  namespace Cypress {
    interface Chainable {
      loginAsPlayer(): Chainable<void>;
      loginAsAdmin(): Chainable<void>;
    }
  }
}

function loginThroughUi(user: TestUser): void {
  const credentials = users[user];

  cy.intercept('POST', '**/api/v1/auth/login').as(`${user}Login`);
  cy.visit('/login');
  cy.get('input[autocomplete="username"]').should('be.visible').type(credentials.username);
  cy.get('input[autocomplete="current-password"]').should('be.visible').type(credentials.password, {
    log: false
  });
  cy.contains('button', 'Se connecter').click();
  cy.wait(`@${user}Login`).its('response.statusCode').should('eq', 200);
  cy.location('pathname').should('eq', credentials.landingPath);
  cy.window().its('localStorage.auth_token').should('be.a', 'string').and('not.be.empty');
}

function loginWithSession(user: TestUser): void {
  cy.session(
    user,
    () => {
      loginThroughUi(user);
    },
    {
      validate() {
        cy.window().its('localStorage.auth_token').should('be.a', 'string').and('not.be.empty');
      }
    }
  );
}

Cypress.Commands.add('loginAsPlayer', () => {
  loginWithSession('player');
});

Cypress.Commands.add('loginAsAdmin', () => {
  loginWithSession('admin');
});

export {};
