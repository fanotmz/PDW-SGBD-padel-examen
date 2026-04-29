# Verification backend - coherence de `peutAjouterJoueurPrive`

## Objet

Verifier que le champ backend `peutAjouterJoueurPrive` expose dans le detail match est coherent avec l'autorisation reelle utilisee par l'endpoint :

- `POST /api/v1/matchs/{matchId}/participants/prive`

Le point sensible a controler est le suivant :

- ne pas afficher l'action si le backend refusera de toute facon pour une raison d'autorisation deja previsible

---

## Ou est calcule `peutAjouterJoueurPrive`

Le champ est calcule dans :

- [MatchPadelService.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/service/MatchPadelService.java:349)

La methode cible est :

- `private boolean peutAjouterJoueurPrive(MatchPadel match)`

Reference precise :

- [MatchPadelService.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/service/MatchPadelService.java:366)

---

## Logique utilisee pour `peutAjouterJoueurPrive`

Le boolen vaut `true` uniquement si :

- le match est `PRIVE`
- le match est `PLANIFIE`
- le match n'est pas complet
- l'utilisateur courant est :
  - organisateur du match
  - ou `admin` selon `currentUserFacade.isAdmin()`

Le test admin utilise :

- [CurrentUserFacade.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/security/CurrentUserFacade.java:69)

Definition actuelle :

- `ROLE_ADMIN_GLOBAL`
- `ROLE_ADMIN_SITE`

Donc `isAdmin()` retourne `true` pour les deux profils.

---

## Logique utilisee par le POST prive

L'endpoint utilise le service :

- [ParticipationService.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/service/ParticipationService.java:93)

Dans `ajouterJoueurParOrganisateur(...)`, la logique actuelle est :

- refus si match public
- refus si match non `PLANIFIE`
- autorise si :
  - `currentUserFacade.isAdmin()` vaut `true`
  - ou le joueur courant est l'organisateur

Puis le backend continue les autres controles :

- joueur existe
- joueur deja inscrit
- place disponible

---

## Alignement entre detail et POST

### 1. Organisateur

Oui, c'est aligne.

Le detail et le POST autorisent :

- l'organisateur du match

### 2. Admin global

Oui, c'est aligne.

Le detail et le POST autorisent :

- `ROLE_ADMIN_GLOBAL`

### 3. Statut du match

Oui, c'est aligne.

Le detail exige :

- `PLANIFIE`

Le POST exige aussi :

- `PLANIFIE`

### 4. Match complet

Oui, c'est aligne pour l'affichage principal.

Le detail masque l'action si le match est complet.
Le POST refuse aussi ensuite si le match est complet.

---

## Divergence metier identifiee : `ADMIN_SITE`

### Constat

Il existe un point important a signaler.

Aujourd'hui :

- `peutAjouterJoueurPrive` donne `true` a tout utilisateur pour lequel `currentUserFacade.isAdmin()` vaut `true`
- le POST prive accepte lui aussi tout utilisateur pour lequel `currentUserFacade.isAdmin()` vaut `true`

Or `isAdmin()` couvre :

- `ROLE_ADMIN_GLOBAL`
- `ROLE_ADMIN_SITE`

sans verifier le perimetre de site.

### Consequence

Un `ADMIN_SITE` hors perimetre peut actuellement :

- voir l'action `Ajouter le joueur`
- et executer le POST prive avec succes

si aucune autre regle ne le bloque.

### Verification du controle de perimetre

Le projet possede bien un service de controle de perimetre :

- [ServiceAutorisationAdmin.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/security/ServiceAutorisationAdmin.java:23)

Ce service :

- autorise `ADMIN_GLOBAL`
- autorise `ADMIN_SITE` uniquement si son site autorise correspond au site demande

Mais ce service n'est pas utilise dans :

- `MatchPadelService.peutAjouterJoueurPrive(...)`
- `ParticipationService.ajouterJoueurParOrganisateur(...)`

### Conclusion sur `ADMIN_SITE`

Le boolen et le POST sont coherents entre eux, mais pas avec l'exigence metier suivante :

- ne pas donner `true` a un admin site hors perimetre

En l'etat actuel :

- un `ADMIN_SITE` hors site est trop largement autorise

---

## Statuts backend existants

L'enum actuelle est :

- [MatchStatut.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/model/enums/MatchStatut.java:1)

Valeurs presentes :

- `PLANIFIE`
- `ANNULE`

Conclusion :

- le refus du POST prive si le match n'est pas `PLANIFIE` est coherent avec l'etat actuel du modele
- aucun autre statut n'a ete trouve dans l'enum actuelle

---

## Conclusion finale

### Ce qui est correct

- organisateur : aligne
- admin global : aligne
- statut `PLANIFIE` : aligne
- match complet : aligne

### Ce qui n'est pas correct au regard du besoin precise

- `ADMIN_SITE` n'est pas limite aujourd'hui au site du match

### Formulation precise

Il n'y a pas de divergence entre :

- le calcul de `peutAjouterJoueurPrive`
- et le POST prive

Mais il existe une divergence entre :

- le comportement actuel du backend
- et l'attendu fonctionnel si l'on veut restreindre `ADMIN_SITE` a son perimetre de site

### Impact

Avant validation definitive de l'issue, il faut decider si :

1. le comportement actuel est accepte tel quel
2. ou une correction backend ciblee est necessaire pour brancher le controle de perimetre `ADMIN_SITE`

Ce document est un constat de verification.
Aucune modification supplementaire n'a ete appliquee.

---

## Decision de suite

La verification a conduit a la decision suivante :

- l'issue 12 ne peut pas etre validee definitivement avec un `ADMIN_SITE` autorise hors perimetre
- la correction doit etre faite dans cette issue
- le frontend ne doit pas contourner ce probleme
- le backend doit rester la source de verite pour l'autorisation

Objectif de la correction :

- un `ADMIN_SITE` ne doit pouvoir ajouter un joueur a un match prive que si le match appartient a son site autorise
- un `ADMIN_SITE` hors perimetre ne doit pas voir l'action via `peutAjouterJoueurPrive`
- un `ADMIN_SITE` hors perimetre ne doit pas pouvoir executer le `POST /participants/prive` avec succes

---

## Fichiers et methodes impactes

### Backend

- [ParticipationService.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/service/ParticipationService.java:93)
  - methode `ajouterJoueurParOrganisateur(...)`
- [MatchPadelService.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/service/MatchPadelService.java:366)
  - methode `peutAjouterJoueurPrive(...)`
- [ServiceAutorisationAdmin.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/security/ServiceAutorisationAdmin.java:23)
  - service existant a reutiliser pour le perimetre `ADMIN_SITE`

### Tests

- [ParticipationServiceTest.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/test/java/be/ephec/padel/backend/unit/service/ParticipationServiceTest.java:1)
- [MatchPadelServiceTest.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/test/java/be/ephec/padel/backend/unit/service/MatchPadelServiceTest.java:1)

---

## Proposition de resolution

### Regle cible

L'action `Ajouter un joueur` doit etre autorisee si :

- l'utilisateur courant est l'organisateur du match
- ou l'utilisateur courant est `ADMIN_GLOBAL`
- ou l'utilisateur courant est `ADMIN_SITE` et son perimetre correspond au site du match

### Strategie proposee

1. Corriger `ParticipationService.ajouterJoueurParOrganisateur(...)`

- remplacer le simple test `currentUserFacade.isAdmin()`
- distinguer explicitement :
  - organisateur
  - `ADMIN_GLOBAL`
  - `ADMIN_SITE` du bon site
- refuser un `ADMIN_SITE` hors perimetre

2. Corriger `MatchPadelService.peutAjouterJoueurPrive(...)`

- reutiliser exactement la meme logique d'autorisation
- garder le meme filtre metier d'affichage :
  - match `PRIVE`
  - match `PLANIFIE`
  - match non complet
- si l'utilisateur n'est pas autorise, retourner simplement `false`
- ne pas faire echouer le `GET /matchs/:id` juste parce qu'il ne peut pas ajouter un joueur

3. Reutiliser `ServiceAutorisationAdmin`

- ne pas dupliquer la logique de perimetre si le service existant peut etre reutilise proprement
- si besoin, ajouter une methode ciblee de type boolenne pour tester le perimetre sans lever d'exception
- conserver la methode actuelle qui leve une exception pour les cas ou elle reste utile ailleurs

### Forme prudente de l'implementation

La resolution la plus propre serait de centraliser une decision backend du type :

- organisateur
- ou admin autorise sur le site du match

Puis :

- l'utiliser dans `ParticipationService` pour autoriser ou refuser le POST
- l'utiliser dans `MatchPadelService` pour calculer `peutAjouterJoueurPrive`

Ainsi :

- le detail et le POST restent strictement alignes
- `ADMIN_GLOBAL` reste autorise
- `ADMIN_SITE` est limite au bon perimetre
- le frontend continue simplement a lire `peutAjouterJoueurPrive`

---

## Tests a couvrir apres correction

Tests attendus :

- `ADMIN_GLOBAL` peut ajouter
- organisateur peut ajouter
- `ADMIN_SITE` du bon site peut ajouter
- `ADMIN_SITE` hors perimetre ne peut pas ajouter
- `peutAjouterJoueurPrive = true` pour `ADMIN_SITE` du bon site
- `peutAjouterJoueurPrive = false` pour `ADMIN_SITE` hors perimetre

---

## Portee de la correction

Cette correction reste dans le scope de l'issue 12 car elle concerne directement :

- l'autorisation reelle de l'action `Ajouter un joueur a un match prive`
- la coherence entre le detail match et l'endpoint prive

Elle ne demande pas :

- de changement frontend de contournement
- de refactor global
- d'extension fonctionnelle hors issue
