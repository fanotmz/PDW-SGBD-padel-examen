export interface MatchStatusView {
  statut?: string | null;
  statutTemporel?: string | null;
  dateDebut?: string | null;
  complet?: boolean | null;
  passe?: boolean | null;
}

export type MatchUserRole = 'ORGANISATEUR' | 'PARTICIPANT' | null | undefined;

export interface MatchBadgeView {
  label: string;
  className: string;
}

export function getTemporalStatusLabel(match: MatchStatusView): string {
  if (match.statut === 'ANNULE') {
    return 'Annulé';
  }

  switch (match.statutTemporel) {
    case 'PASSE':
      return 'Déjà joué';
    case 'AUJOURD_HUI':
      return 'Aujourd’hui';
    case 'FUTUR':
      return 'À venir';
    default:
      break;
  }

  if (match.passe === true) {
    return 'Déjà joué';
  }

  if (match.dateDebut) {
    const matchDay = getDayTimestamp(match.dateDebut);
    const today = getCurrentDayTimestamp();

    if (matchDay != null && matchDay < today) {
      return 'Déjà joué';
    }

    if (matchDay != null && matchDay === today) {
      return 'Aujourd’hui';
    }
  }

  return 'À venir';
}

export function getMatchStatusLabel(match: MatchStatusView): string {
  return getTemporalStatusLabel(match);
}

export function getTemporalStatusClassName(match: MatchStatusView): string {
  if (match.statut === 'ANNULE') {
    return 'is-cancelled';
  }

  switch (getTemporalStatusLabel(match)) {
    case 'Déjà joué':
      return 'temporal-passe';
    case 'Aujourd’hui':
      return 'temporal-aujourd_hui';
    case 'À venir':
      return 'temporal-futur';
    default:
      return '';
  }
}

export function getMatchStatusClassMap(match: MatchStatusView): Record<string, boolean> {
  const className = getTemporalStatusClassName(match);

  return {
    'is-cancelled': className === 'is-cancelled',
    'temporal-passe': className === 'temporal-passe',
    'temporal-aujourd_hui': className === 'temporal-aujourd_hui',
    'temporal-futur': className === 'temporal-futur'
  };
}

export function getUserMatchRoleLabel(role: MatchUserRole): string {
  if (role === 'ORGANISATEUR') {
    return 'Organisateur';
  }

  if (role === 'PARTICIPANT') {
    return 'Déjà inscrit';
  }

  return '';
}

export function getUserMatchRoleClassName(role: MatchUserRole): string {
  if (role === 'ORGANISATEUR') {
    return 'is-organizer';
  }

  if (role === 'PARTICIPANT') {
    return 'is-registered';
  }

  return '';
}

export function getAvailabilityLabel(match: MatchStatusView): string {
  return match.complet ? 'Complet' : 'Places disponibles';
}

export function getAvailabilityClassName(match: MatchStatusView): string {
  return match.complet ? 'is-full' : 'is-open';
}

export function getSecondaryMatchBadge(match: MatchStatusView, role: MatchUserRole): MatchBadgeView {
  if (isTerminalMatchStatus(match)) {
    return {
      label: '',
      className: ''
    };
  }

  const userLabel = getUserMatchRoleLabel(role);

  if (userLabel) {
    return {
      label: userLabel,
      className: getUserMatchRoleClassName(role)
    };
  }

  return {
    label: getAvailabilityLabel(match),
    className: getAvailabilityClassName(match)
  };
}

export function isTerminalMatchStatus(match: MatchStatusView): boolean {
  return match.statut === 'ANNULE' || getTemporalStatusLabel(match) === 'Déjà joué';
}

function getCurrentDayTimestamp(): number {
  const now = new Date();

  return new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
}

function getDayTimestamp(dateValue: string): number | null {
  const [year, month, day] = dateValue.slice(0, 10).split('-').map((value) => Number(value));

  if ([year, month, day].some(Number.isNaN)) {
    return null;
  }

  return new Date(year, month - 1, day).getTime();
}
