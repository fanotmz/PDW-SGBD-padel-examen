# PDW / SGBD — Padel

API REST Spring Boot + SQL Server pour la gestion d’un système de padel : sites, terrains, joueurs, matchs, participations et paiements. Le projet inclut un espace d’administration (endpoints protégés) et une documentation OpenAPI/Swagger.

## Sommaire
- Présentation
- Stack technique
- Structure du dépôt
- Prérequis
- Configuration
  - Base de données
  - Secrets et variables d’environnement
  - Sécurité (Basic Auth)
- Lancement
  - Backend
  - Tests
- Swagger / OpenAPI
- Endpoints Admin (principaux)
- Exemples d’utilisation (curl)
- Conventions et qualité
- Dépannage
- Roadmap

---

## Présentation
Ce projet expose une API versionnée (`/api/v1`) permettant de gérer :
- des sites (club/lieu),
- des terrains,
- des joueurs,
- des matchs,
- des participations,
- des paiements.

Les endpoints d’administration sont regroupés sous `/api/v1/admin/**` et sont protégés par authentification Basic (Spring Security).

---

## Stack technique
Backend :
- Java + Spring Boot
- Spring Web (API REST)
- Spring Data JPA / Hibernate (persistence)
- Spring Security (Basic Auth, stateless)
- springdoc-openapi (Swagger UI + OpenAPI)

Base de données :
- SQL Server

Frontend  :
- Angular (dossier `frontend/`)

---

## Structure du dépôt
- `backend/` : application Spring Boot (API REST)
- `frontend/` : application Angular (optionnel)
- `docs/` : documentation (architecture, schémas, etc.)
- `planning/` : planning / organisation du projet

---

## Prérequis
- Java (version définie par le projet)
- Maven
- SQL Server (local ou via Docker)
- (Optionnel) Docker Desktop si vous utilisez des containers
- (Optionnel) Node.js + npm + Angular CLI pour le frontend

---

## Configuration

### Base de données
Le backend se connecte à SQL Server via la configuration Spring (ex : `application.properties`).

Selon votre environnement, vous devrez :
- créer la base,
- configurer l’utilisateur et les droits,
- vérifier que SQL Server accepte les connexions TCP/IP (port 1433 le plus souvent),
- adapter l’URL JDBC et les credentials.

### Secrets et variables d’environnement
Recommandation : ne pas versionner les secrets (DB username/password).

Deux approches simples :
- Variables d’environnement (ex : `DB_USER`, `DB_PASSWORD`)
- Profil local Spring : `application-local.properties` ignoré par Git (via `.gitignore`)

### Sécurité (Basic Auth)
Les endpoints admin `/api/v1/admin/**` sont protégés par Spring Security (Basic Auth).

Identifiants admin par défaut (si non surchargés par propriétés/variables) :
- username : `admin`
- password : `admin123`

Swagger reste accessible (public en dev) mais les endpoints admin nécessitent une authentification.

---

## Lancement

### Backend (dev)
Depuis le dossier `backend/` :

```bash
mvn spring-boot:run
