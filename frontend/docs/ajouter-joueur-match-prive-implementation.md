# Implementation - Issue 12 Ajouter joueur a match prive

## Objectif

Permettre l'ajout manuel d'un joueur a un match prive depuis la page detail `/matchs/:id`, sans deplacer la logique metier dans Angular.

Le backend reste responsable :

- des autorisations
- des validations business
- des refus fonctionnels

Le frontend :

- affiche l'action uniquement si le detail du match l'autorise
- envoie la demande au backend
- affiche le succes ou l'erreur
- recharge le detail apres succes

---

## Perimetre implemente

Cette implementation couvre :

- l'ajout du champ `peutAjouterJoueurPrive` dans le detail match
- le calcul backend de ce champ
- l'alignement du POST prive sur la condition `PLANIFIE`
- l'ajout du formulaire minimal dans la page detail
- l'appel frontend vers l'endpoint prive existant
- le rechargement du detail apres succes

Cette implementation ne couvre pas :

- recherche joueur
- autocomplete
- liste deroulante de joueurs
- nouvelle page
- refactor global

---

## Backend

### 1. DTO de detail match

Le DTO de detail match expose maintenant :

- `peutAjouterJoueurPrive: boolean`

Fichier :

- [MatchDetailDto.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/dto/response/MatchDetailDto.java:33)

Ce champ sert uniquement a l'affichage frontend.

---

### 2. Mapping du detail

Le mapper du detail transporte maintenant ce boolen vers la reponse JSON.

Fichier :

- [MatchDetailMapper.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/mapper/MatchDetailMapper.java:16)

---

### 3. Calcul de `peutAjouterJoueurPrive`

Le calcul est fait dans le service de detail match.

Fichier :

- [MatchPadelService.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/service/MatchPadelService.java:349)

Regle implemente :

`peutAjouterJoueurPrive = true` uniquement si :

- le match est `PRIVE`
- le match est `PLANIFIE`
- le match n'est pas complet
- l'utilisateur courant est :
  - organisateur
  - ou `ADMIN_GLOBAL`
  - ou `ADMIN_SITE` avec un perimetre correspondant au site du match

Le calcul reste donc coherent avec l'action backend reelle.

---

### 3bis. Correction de coherence `ADMIN_SITE`

Une correction backend ciblee a ete ajoutee avant validation finale de l'issue.

Probleme detecte :

- un `ADMIN_SITE` etait auparavant autorise trop largement via un simple test `isAdmin()`
- cela permettait a un `ADMIN_SITE` hors perimetre :
  - de voir l'action
  - et d'executer le POST prive

Correction appliquee :

- reutilisation du service existant [ServiceAutorisationAdmin.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/security/ServiceAutorisationAdmin.java:1)
- ajout d'une methode booleenne :
  - `peutAdministrerSite(siteId)`

Reference :

- [ServiceAutorisationAdmin.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/security/ServiceAutorisationAdmin.java:46)

Semantique :

- `true` pour `ADMIN_GLOBAL`
- `true` pour `ADMIN_SITE` uniquement si son site autorise correspond au site du match
- `false` sinon

Cette methode permet :

- d'autoriser ou refuser le POST prive
- de calculer `peutAjouterJoueurPrive`
- sans faire echouer le GET detail quand l'utilisateur n'a simplement pas le droit d'ajouter un joueur

---

### 4. Endpoint prive existant conserve

L'endpoint utilise reste :

- `POST /api/v1/matchs/{matchId}/participants/prive`

Fichier :

- [ParticipationController.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/controller/ParticipationController.java:1)

Body envoye :

```json
{
  "joueurMatriculeAAjouter": "G0002"
}
```

---

### 5. Alignement du POST prive

Le service backend d'ajout prive refuse maintenant aussi l'ajout si le match n'est pas `PLANIFIE`.

Fichier :

- [ParticipationService.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/service/ParticipationService.java:103)

But :

- eviter une divergence entre
  - ce que le detail affiche
  - et ce que le POST accepte reellement

La logique finale d'autorisation du POST prive est maintenant :

- organisateur du match
- ou `ADMIN_GLOBAL`
- ou `ADMIN_SITE` du bon site

Reference :

- [ParticipationService.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/service/ParticipationService.java:110)

Les controles backend existants restent en place :

- match introuvable
- match annule
- refus si match public
- refus si non organisateur et non admin
- refus si joueur introuvable
- refus si joueur deja inscrit
- refus si match complet

---

## Frontend

### 1. Modele detail match

Le modele TypeScript du detail match expose maintenant :

- `peutAjouterJoueurPrive: boolean`

Fichier :

- [match-detail.models.ts](/C:/Users/fanoa/PDW-SGBD-padel-examen/frontend/src/app/core/matches/match-detail.models.ts:21)

---

### 2. Service de participation

Le service existant a ete etendu.

Fichier :

- [match-participation.service.ts](/C:/Users/fanoa/PDW-SGBD-padel-examen/frontend/src/app/core/matches/match-participation.service.ts:18)

Nouvelle methode :

- `ajouterJoueurPrive(matchId, joueurMatriculeAAjouter)`

Elle appelle :

- `POST /matchs/{matchId}/participants/prive`

Le service existant de participation publique n'a pas ete remplace.

---

### 3. Affichage conditionnel dans le detail

La page detail utilise maintenant directement :

- `detail.peutAjouterJoueurPrive`

Fichier :

- [match-detail-page.component.ts](/C:/Users/fanoa/PDW-SGBD-padel-examen/frontend/src/app/features/matches/match-detail/match-detail-page.component.ts:49)

Computed utilise :

- `canShowPrivateAddForm`

Le frontend ne deduit pas lui-meme si l'utilisateur est organisateur.
Le frontend ne deduit pas non plus le perimetre `ADMIN_SITE`.

---

### 4. Formulaire minimal ajoute

Le formulaire apparait uniquement si `peutAjouterJoueurPrive === true`.

Fichier :

- [match-detail-page.component.html](/C:/Users/fanoa/PDW-SGBD-padel-examen/frontend/src/app/features/matches/match-detail/match-detail-page.component.html:146)

Contenu :

- un champ texte `Matricule du joueur`
- un bouton `Ajouter le joueur`

Le formulaire reste volontairement simple :

- pas d'autocomplete
- pas de recherche joueur
- pas de select

---

### 5. Gestion du submit

Le composant detail gere maintenant :

- l'etat de chargement de l'ajout prive
- le message de succes
- le message d'erreur backend
- les details d'erreur backend si presents

Fichier :

- [match-detail-page.component.ts](/C:/Users/fanoa/PDW-SGBD-padel-examen/frontend/src/app/features/matches/match-detail/match-detail-page.component.ts:103)

Comportement :

1. l'utilisateur saisit un matricule
2. Angular appelle `ajouterJoueurPrive(...)`
3. si succes :
   - message de succes
   - reset du champ
   - rechargement du detail match
4. si erreur :
   - le champ n'est pas reset
   - le message backend est affiche

---

### 6. Styles

Un style minimal a ete ajoute pour :

- le bloc de formulaire
- le champ texte
- le bouton d'action

Fichier :

- [match-detail-page.component.css](/C:/Users/fanoa/PDW-SGBD-padel-examen/frontend/src/app/features/matches/match-detail/match-detail-page.component.css:160)

---

## Fichiers modifies

### Backend

- [MatchDetailDto.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/dto/response/MatchDetailDto.java:1)
- [MatchDetailMapper.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/mapper/MatchDetailMapper.java:1)
- [ServiceAutorisationAdmin.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/security/ServiceAutorisationAdmin.java:1)
- [MatchPadelService.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/service/MatchPadelService.java:1)
- [ParticipationService.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/service/ParticipationService.java:1)
- [MatchControllerTest.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/test/java/be/ephec/padel/backend/web/controller/MatchControllerTest.java:1)
- [MatchPadelServiceTest.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/test/java/be/ephec/padel/backend/unit/service/MatchPadelServiceTest.java:1)
- [ParticipationServiceTest.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/test/java/be/ephec/padel/backend/unit/service/ParticipationServiceTest.java:1)
- [ParticipationServiceMontantAttenduTest.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/test/java/be/ephec/padel/backend/unit/service/ParticipationServiceMontantAttenduTest.java:1)
- [ParticipationServiceRejoindrePublicTest.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/test/java/be/ephec/padel/backend/unit/service/ParticipationServiceRejoindrePublicTest.java:1)

### Frontend

- [match-detail.models.ts](/C:/Users/fanoa/PDW-SGBD-padel-examen/frontend/src/app/core/matches/match-detail.models.ts:1)
- [match-participation.service.ts](/C:/Users/fanoa/PDW-SGBD-padel-examen/frontend/src/app/core/matches/match-participation.service.ts:1)
- [match-detail-page.component.ts](/C:/Users/fanoa/PDW-SGBD-padel-examen/frontend/src/app/features/matches/match-detail/match-detail-page.component.ts:1)
- [match-detail-page.component.html](/C:/Users/fanoa/PDW-SGBD-padel-examen/frontend/src/app/features/matches/match-detail/match-detail-page.component.html:1)
- [match-detail-page.component.css](/C:/Users/fanoa/PDW-SGBD-padel-examen/frontend/src/app/features/matches/match-detail/match-detail-page.component.css:1)

---

## Verifications realisees

Backend :

- `.\mvnw.cmd "-Dtest=MatchControllerTest,MatchPadelServiceTest,ParticipationServiceTest" test` OK
- `.\mvnw.cmd "-Dtest=ParticipationServiceTest,MatchPadelServiceTest,MatchControllerTest" test` OK
- `.\mvnw.cmd -DskipTests package` OK

Frontend :

- `npm.cmd run build` OK

Points verifies :

- le detail match expose `peutAjouterJoueurPrive`
- le formulaire apparait uniquement si ce champ vaut `true`
- l'appel passe bien par l'endpoint prive existant
- le detail est recharge apres succes
- le flux existant `Rejoindre le match` public reste en place
- `ADMIN_GLOBAL` est autorise
- l'organisateur est autorise
- `ADMIN_SITE` du bon site est autorise
- `ADMIN_SITE` hors perimetre ne voit plus l'action via `peutAjouterJoueurPrive`
- `ADMIN_SITE` hors perimetre ne peut plus reussir le POST prive

---

## Resultat

Le detail match prive peut maintenant afficher proprement l'action `Ajouter le joueur` sans reconstituer les autorisations cote Angular.

Le backend reste la source de verite :

- pour savoir si l'action doit etre visible
- pour accepter ou refuser l'ajout
- pour renvoyer le message fonctionnel a afficher

Le frontend continue simplement a lire :

- `peutAjouterJoueurPrive`

Il ne reconstitue ni :

- l'identite de l'organisateur
- ni le perimetre `ADMIN_SITE`
