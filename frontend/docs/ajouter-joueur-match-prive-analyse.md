# Analyse - Issue 12 Ajouter joueur a match prive

## Contexte

Projet :

- PDW / SGBD gestion de matchs de padel

Issue etudiee :

- Issue 12 - Ajouter joueur a match prive

Contraintes de l'analyse :

- ne rien coder pour l'instant
- ne modifier aucun fichier applicatif
- ne pas changer le scope
- ne pas ajouter de logique metier dans le frontend
- laisser le backend responsable des regles et autorisations

Decision validee pour preparer l'implementation :

- une legere modification backend est acceptee
- ajout cible dans le DTO de detail match :
  - `peutAjouterJoueurPrive: boolean`
- ce champ sert uniquement a l'affichage frontend
- il ne remplace pas les verifications backend de l'endpoint POST prive

Rappel fonctionnel SGBD :

- pour un match prive, le responsable qui a fait la reservation ajoute lui-meme les autres joueurs
- pour un match public, l'organisateur ne peut pas reserver pour un autre joueur
- l'ajout manuel de joueur concerne donc uniquement les matchs prives

---

## Constat du code existant

### Frontend

Le frontend Angular dispose deja de :

- login JWT
- `AuthGuard`
- `jwtInterceptor`
- `httpErrorInterceptor`
- `/matchs`
- `/matchs/:id`
- `/me/matchs`
- `/me/matchs/organises`

Fonctionnalites deja en place autour des matchs :

- liste des matchs publics
- detail de match
- rejoindre un match public
- gestion des erreurs backend
- filtres frontend
- affichage conditionnel des liens vers le detail
- corrections UX sur badges, statuts, dates

### Backend

Le backend contient deja une logique de participation centralisee dans :

- [ParticipationController.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/controller/ParticipationController.java:1)
- [ParticipationService.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/service/ParticipationService.java:1)

Le detail de match est expose via :

- [MatchDetailDto.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/dto/response/MatchDetailDto.java:1)

Le modele frontend correspondant existe deja :

- [match-detail.models.ts](/C:/Users/fanoa/PDW-SGBD-padel-examen/frontend/src/app/core/matches/match-detail.models.ts:1)

Le frontend a deja un service pour les participations publiques :

- [match-participation.service.ts](/C:/Users/fanoa/PDW-SGBD-padel-examen/frontend/src/app/core/matches/match-participation.service.ts:1)

Le flux de detail match est le point d'injection le plus cible pour la modification validee :

- [MatchController.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/controller/MatchController.java:1)
- [MatchPadelService.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/service/MatchPadelService.java:1)
- [MatchDetailMapper.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/mapper/MatchDetailMapper.java:1)

---

## Endpoint backend trouve

Oui, l'endpoint existe deja.

Endpoint :

- `POST /api/v1/matchs/{matchId}/participants/prive`

Reference :

- [ParticipationController.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/controller/ParticipationController.java:1)

Signature backend :

```java
@PostMapping("/{matchId}/participants/prive")
public ResponseEntity<ParticipationDto> ajouterJoueurPrive(
        @PathVariable Long matchId,
        @Valid @RequestBody AddPlayerToPrivateMatchRequest req)
```

Body attendu :

```json
{
  "joueurMatriculeAAjouter": "..."
}
```

Request DTO :

- [AddPlayerToPrivateMatchRequest.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/dto/request/AddPlayerToPrivateMatchRequest.java:1)

Retour :

- `ParticipationDto`

Conclusion :

- aucun endpoint backend supplementaire n'est necessaire pour respecter l'enonce

---

## Verifications backend utiles

Le backend gere deja les regles metier et les autorisations dans :

- [ParticipationService.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/service/ParticipationService.java:1)

La methode utile est :

- `ajouterJoueurParOrganisateur(Long matchId, String joueurMatriculeAAjouter)`

Regles deja prises en charge par le backend :

- match introuvable
- match annule
- refus si le match est public
- refus si l'utilisateur courant n'est ni organisateur ni admin
- refus si le joueur a ajouter n'existe pas
- refus si le joueur est deja inscrit
- refus si le match est complet

Conclusion :

- il ne faut pas reimplementer ces controles dans le frontend

---

## Service frontend de participation

Etat actuel :

- le frontend possede deja `MatchParticipationService`
- il couvre seulement `rejoindreMatchPublic(matchId)`

Reference :

- [match-participation.service.ts](/C:/Users/fanoa/PDW-SGBD-padel-examen/frontend/src/app/core/matches/match-participation.service.ts:1)

Conclusion :

- le service est partiellement adapte
- il manque seulement une methode pour l'appel prive
- il n'est pas necessaire de creer un nouveau service

---

## Le frontend peut-il savoir proprement que l'utilisateur connecte est l'organisateur ?

### Reponse courte

Non, pas proprement avec les donnees actuelles.

### Pourquoi

Le detail du match expose :

- `organisateurMatricule`

References :

- [MatchDetailDto.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/dto/response/MatchDetailDto.java:1)
- [match-detail.models.ts](/C:/Users/fanoa/PDW-SGBD-padel-examen/frontend/src/app/core/matches/match-detail.models.ts:1)

Mais le frontend d'authentification ne stocke que :

- le JWT
- un bool d'authentification

References :

- [auth.service.ts](/C:/Users/fanoa/PDW-SGBD-padel-examen/frontend/src/app/core/auth/auth.service.ts:1)
- [auth.models.ts](/C:/Users/fanoa/PDW-SGBD-padel-examen/frontend/src/app/core/auth/auth.models.ts:1)

La reponse de login ne contient pas :

- le matricule du joueur
- l'id du joueur
- un flag "est organisateur"

Le JWT porte comme subject :

- le `login` utilisateur

Reference :

- [JwtService.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/security/JwtService.java:1)

Or, dans le modele backend :

- `User.login` et `Joueur.matricule` sont deux donnees distinctes

References :

- [User.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/model/entities/User.java:1)
- [Joueur.java](/C:/Users\fanoa\PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/model/entities/Joueur.java:1)

### Conclusion

Le frontend ne peut pas comparer de facon fiable :

- l'utilisateur connecte
- et `organisateurMatricule`

Donc il ne peut pas savoir proprement, avec les donnees existantes, que l'utilisateur courant est l'organisateur.

---

## Decision retenue

Pour resoudre proprement ce point sans ajouter de logique metier cote Angular, la decision retenue est :

- enrichir le DTO de detail match avec `peutAjouterJoueurPrive`
- calculer ce boolen cote backend
- l'utiliser uniquement pour afficher ou masquer l'action frontend

Ce boolen :

- ne remplace pas l'autorisation backend
- ne remplace pas les controles de visibilite, statut, places ou organisateur
- ne change pas l'endpoint POST existant

---

## Comment calculer `peutAjouterJoueurPrive`

Le calcul doit etre fait dans le backend, dans le flux de detail match, idealement dans :

- [MatchPadelService.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/service/MatchPadelService.java:1)

Regle prudente proposee pour `true` :

- match `PRIVE`
- match non `ANNULE`
- match non complet
- utilisateur courant = organisateur ou admin

En pratique, le calcul doit s'aligner sur les regles deja appliquees dans :

- [ParticipationService.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/service/ParticipationService.java:1)

mais seulement pour la partie qui ne depend pas encore du joueur a ajouter.

Important :

- je n'ajouterais pas de regle supplementaire "match passe" si elle n'existe pas deja dans l'endpoint POST
- le boolen d'affichage doit rester coherent avec le comportement backend reel

---

## Solution prudente retenue

- le backend renvoie `peutAjouterJoueurPrive`
- le frontend affiche le formulaire uniquement si `peutAjouterJoueurPrive === true`
- le submit appelle l'endpoint POST prive existant
- apres succes, le detail du match est recharge
- en cas d'erreur, le message backend est affiche proprement

Cette approche :

- respecte le scope
- evite de reconstituer l'autorisation cote Angular
- garde le backend comme source de verite
- reste ciblee sur le detail match

---

## Proposition d'implementation courte et prudente

Sans coder pour l'instant, l'implementation la plus sure serait :

1. Ajouter `peutAjouterJoueurPrive` a `MatchDetailDto`
2. Calculer ce champ dans le backend au moment de construire le detail match
3. Reutiliser la page detail existante `/matchs/:id`
4. Etendre `MatchParticipationService` avec une methode pour :
   - `POST /api/v1/matchs/{id}/participants/prive`
5. Ajouter dans le detail du match prive un petit formulaire simple :
   - un champ texte `joueurMatriculeAAjouter`
   - un bouton `Ajouter le joueur`
6. Au submit :
   - envoyer `{ joueurMatriculeAAjouter }`
   - afficher le message backend si erreur
   - afficher un message de succes si OK
   - recharger le detail du match
7. Ne pas ajouter d'autocomplete, de recherche joueur, ni de logique supplementaire hors scope

Ce que le frontend ne ferait pas :

- verifier que le joueur existe
- verifier qu'il reste une place
- verifier qu'il n'est pas deja inscrit
- verifier que l'utilisateur est bien organisateur

Tout cela resterait cote backend.

---

## Fichiers probablement concernes

### Frontend

- [match-detail.models.ts](/C:/Users/fanoa/PDW-SGBD-padel-examen/frontend/src/app/core/matches/match-detail.models.ts:1)
- [match-participation.service.ts](/C:/Users/fanoa/PDW-SGBD-padel-examen/frontend/src/app/core/matches/match-participation.service.ts:1)
- [match-participation.models.ts](/C:/Users/fanoa/PDW-SGBD-padel-examen/frontend/src/app/core/matches/match-participation.models.ts:1)  
  Reutilisable tel quel pour la reponse. Un type request optionnel pourrait etre ajoute si souhaite, sans obligation.
- [match-detail-page.component.ts](/C:/Users/fanoa/PDW-SGBD-padel-examen/frontend/src/app/features/matches/match-detail/match-detail-page.component.ts:1)
- [match-detail-page.component.html](/C:/Users/fanoa/PDW-SGBD-padel-examen/frontend/src/app/features/matches/match-detail/match-detail-page.component.html:1)
- [match-detail-page.component.css](/C:/Users/fanoa/PDW-SGBD-padel-examen/frontend/src/app/features/matches/match-detail/match-detail-page.component.css:1)

### Backend

- [MatchDetailDto.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/dto/response/MatchDetailDto.java:1)
- [MatchDetailMapper.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/mapper/MatchDetailMapper.java:1)
- [MatchPadelService.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/main/java/be/ephec/padel/backend/service/MatchPadelService.java:1)
- [MatchControllerTest.java](/C:/Users/fanoa/PDW-SGBD-padel-examen/backend/backend/src/test/java/be/ephec/padel/backend/web/controller/MatchControllerTest.java:1)

---

## Risques

1. Coherence entre affichage et backend

Le boolen `peutAjouterJoueurPrive` doit rester strictement coherent avec les regles backend reelles.

Si on ajoute une condition d'affichage qui n'est pas verifiee par le POST prive, on cree une divergence.

2. Ambiguite entre organisateur et admin

Le backend autorise :

- l'organisateur
- ou un admin

Alors que l'enonce parle surtout du responsable / organisateur.

Il faut valider si cette difference est acceptable cote UX ou simplement assumee comme comportement backend existant.

3. Saisie par matricule

Le backend attend :

- `joueurMatriculeAAjouter`

Donc la version la plus prudente est un simple champ texte.

Si le besoin produit attend une recherche utilisateur ou une liste deroulante, ce serait un scope plus large.

4. Tests backend a adapter

`MatchDetailDto` est instancie explicitement dans les tests controleur.

Donc l'ajout du nouveau champ imposera une mise a jour ciblee des fixtures et assertions.

5. Visibilite du match prive

Il faut verifier en pratique que l'acces a `/matchs/:id` pour un match prive est deja conforme aux autorisations attendues.

---

## Points a valider avant codage

1. Le mode de saisie attendu

- un simple champ `matricule joueur` est-il acceptable pour l'issue 12 ?

2. La semantique exacte de `peutAjouterJoueurPrive`

- doit-il etre `true` pour l'admin aussi ?
- la reponse prudente est oui, puisque le backend l'autorise deja

3. L'alignement avec le POST prive

- faut-il rester strictement aligne sur les regles deja appliquees par le backend
- la reponse prudente est oui

4. Le perimetre UX

- faut-il rester sur un simple formulaire minimal
- sans recherche joueur
- sans liste de joueurs disponibles

5. Le pattern d'integration

- reutiliser le meme schema que "rejoindre un match public" :
  - etat de chargement
  - message succes
  - message erreur backend
  - reload du detail apres succes

---

## Conclusion

Le backend fournit deja l'endpoint et les regles necessaires pour l'issue 12.

Le frontend ne peut pas savoir proprement aujourd'hui, avec les donnees existantes, que l'utilisateur connecte est l'organisateur du match prive, car :

- il connait le login JWT
- mais pas le matricule joueur courant
- alors que le detail expose seulement `organisateurMatricule`

La modification ciblee backend validee permet de lever proprement cette limite :

- le detail match renverra `peutAjouterJoueurPrive`
- le frontend affichera l'action uniquement si ce champ vaut `true`
- le backend continuera a verifier toutes les regles au moment du POST prive
- le frontend se limitera a appeler l'API, afficher le succes ou l'erreur, puis recharger le detail

Cette approche est la plus sure avant codage.
