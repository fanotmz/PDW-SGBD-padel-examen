import {
  getAvailabilityClassName,
  getAvailabilityLabel,
  getMatchStatusClassMap,
  getSecondaryMatchBadge,
  getTemporalStatusClassName,
  getTemporalStatusLabel,
  getUserMatchRoleClassName,
  getUserMatchRoleLabel,
  isTerminalMatchStatus
} from './match-status.utils';

describe('match-status.utils', () => {
  describe('getTemporalStatusLabel', () => {
    it('returns the cancelled label first', () => {
      expect(getTemporalStatusLabel({ statut: 'ANNULE', statutTemporel: 'FUTUR' })).toBe('Annulé');
    });

    it('returns the past label from the temporal status', () => {
      expect(getTemporalStatusLabel({ statut: 'PLANIFIE', statutTemporel: 'PASSE' })).toBe('Déjà joué');
    });

    it('returns the today label from the temporal status', () => {
      expect(getTemporalStatusLabel({ statut: 'PLANIFIE', statutTemporel: 'AUJOURD_HUI' })).toBe('Aujourd’hui');
    });

    it('returns the future label from the temporal status', () => {
      expect(getTemporalStatusLabel({ statut: 'PLANIFIE', statutTemporel: 'FUTUR' })).toBe('À venir');
    });

    it('falls back to the future label when no supported status is available', () => {
      expect(getTemporalStatusLabel({ statut: 'INCONNU' })).toBe('À venir');
    });
  });

  describe('status classes', () => {
    it('returns the cancelled class', () => {
      expect(getTemporalStatusClassName({ statut: 'ANNULE' })).toBe('is-cancelled');
    });

    it('returns the past class', () => {
      expect(getTemporalStatusClassName({ statutTemporel: 'PASSE' })).toBe('temporal-passe');
    });

    it('returns the today class', () => {
      expect(getTemporalStatusClassName({ statutTemporel: 'AUJOURD_HUI' })).toBe('temporal-aujourd_hui');
    });

    it('returns the future class', () => {
      expect(getTemporalStatusClassName({ statutTemporel: 'FUTUR' })).toBe('temporal-futur');
    });

    it('builds an exclusive class map', () => {
      expect(getMatchStatusClassMap({ statutTemporel: 'AUJOURD_HUI' })).toEqual({
        'is-cancelled': false,
        'temporal-passe': false,
        'temporal-aujourd_hui': true,
        'temporal-futur': false
      });
    });
  });

  describe('user role labels and classes', () => {
    it('maps organizer role display', () => {
      expect(getUserMatchRoleLabel('ORGANISATEUR')).toBe('Organisateur');
      expect(getUserMatchRoleClassName('ORGANISATEUR')).toBe('is-organizer');
    });

    it('maps participant role display', () => {
      expect(getUserMatchRoleLabel('PARTICIPANT')).toBe('Déjà inscrit');
      expect(getUserMatchRoleClassName('PARTICIPANT')).toBe('is-registered');
    });

    it('returns empty display for missing role', () => {
      expect(getUserMatchRoleLabel(null)).toBe('');
      expect(getUserMatchRoleClassName(undefined)).toBe('');
    });
  });

  describe('availability display', () => {
    it('maps a full match', () => {
      expect(getAvailabilityLabel({ complet: true })).toBe('Complet');
      expect(getAvailabilityClassName({ complet: true })).toBe('is-full');
    });

    it('maps an open match', () => {
      expect(getAvailabilityLabel({ complet: false })).toBe('Places disponibles');
      expect(getAvailabilityClassName({ complet: false })).toBe('is-open');
    });
  });

  describe('getSecondaryMatchBadge', () => {
    it('prioritizes the organizer badge on an active match', () => {
      expect(getSecondaryMatchBadge({ statutTemporel: 'FUTUR', complet: false }, 'ORGANISATEUR')).toEqual({
        label: 'Organisateur',
        className: 'is-organizer'
      });
    });

    it('prioritizes the participant badge on an active match', () => {
      expect(getSecondaryMatchBadge({ statutTemporel: 'AUJOURD_HUI', complet: false }, 'PARTICIPANT')).toEqual({
        label: 'Déjà inscrit',
        className: 'is-registered'
      });
    });

    it('uses availability when there is no user role', () => {
      expect(getSecondaryMatchBadge({ statutTemporel: 'FUTUR', complet: false }, null)).toEqual({
        label: 'Places disponibles',
        className: 'is-open'
      });
      expect(getSecondaryMatchBadge({ statutTemporel: 'FUTUR', complet: true }, undefined)).toEqual({
        label: 'Complet',
        className: 'is-full'
      });
    });

    it('does not return a secondary badge for cancelled or past matches', () => {
      expect(getSecondaryMatchBadge({ statut: 'ANNULE', complet: false }, 'PARTICIPANT')).toEqual({
        label: '',
        className: ''
      });
      expect(getSecondaryMatchBadge({ statutTemporel: 'PASSE', complet: false }, 'ORGANISATEUR')).toEqual({
        label: '',
        className: ''
      });
    });
  });

  describe('isTerminalMatchStatus', () => {
    it('detects cancelled and past matches as terminal', () => {
      expect(isTerminalMatchStatus({ statut: 'ANNULE' })).toBe(true);
      expect(isTerminalMatchStatus({ statutTemporel: 'PASSE' })).toBe(true);
    });

    it('does not treat future or today matches as terminal', () => {
      expect(isTerminalMatchStatus({ statutTemporel: 'FUTUR' })).toBe(false);
      expect(isTerminalMatchStatus({ statutTemporel: 'AUJOURD_HUI' })).toBe(false);
    });
  });
});
