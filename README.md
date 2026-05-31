# Projet PDW / SGBD — Padel Manager

**Auteur : Fano Alessio**

Application web de gestion de réservations de matchs de padel.

## Technologies

- Frontend : Angular
- Backend : Spring Boot / Java 21
- Base de données : SQL Server
- Migrations : Liquibase
- API : REST + Swagger/OpenAPI
- Conteneurisation : Docker Compose

## Structure

- `backend/backend/` : backend Spring Boot, configuration Docker, Liquibase et tests backend
- `frontend/` : application Angular et tests frontend
- `DOCUMENT_EXPLOITATION.md` : commandes pour lancer et tester le projet
- `DOSSIER_ARCHITECTURE.md` : architecture frontend/backend et outils utilisés

## Lancement

Voir `DOCUMENT_EXPLOITATION.md`.

## Architecture

Voir `DOSSIER_ARCHITECTURE.md`.

## Swagger

Après lancement du backend Docker :

http://localhost:8080/swagger-ui/index.html
