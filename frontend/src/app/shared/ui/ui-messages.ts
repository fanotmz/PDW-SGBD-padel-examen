export const UI_MESSAGES = {
  backendUnavailable: 'Backend inaccessible.',
  admin: {
    loading: 'Chargement en cours...'
  },
  createMatch: {
    loadingSites: 'Chargement des sites...',
    incompleteForm: 'Le formulaire est incomplet.',
    success: 'Match créé avec succès.'
  },
  publicMatches: {
    loading: 'Chargement des matchs...',
    loadError: 'Impossible de charger les matchs publics.',
    empty: 'Aucun match public disponible pour le moment.',
    noAvailable: 'Aucun match public disponible à afficher pour ce filtre.',
    noFull: 'Aucun match complet à afficher pour ce filtre.',
    noPast: 'Aucun match déjà joué à afficher pour ce filtre.'
  }
} as const;
