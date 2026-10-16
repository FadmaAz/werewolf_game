[![CI](https://github.com/FadmaAz/werewolf_game/actions/workflows/ci.yml/badge.svg)](https://github.com/FadmaAz/werewolf_game/actions/workflows/ci.yml)

# 🐺 Loup-Garou (Werewolf)

Jeu de société multijoueur **Loup-Garou** en ligne, développé avec **Spring Boot** et **Thymeleaf**.
Les parties se déroulent **en temps réel** (WebSocket / STOMP) : attribution des rôles, phases
nuit/jour minutées, votes et chat d'équipe.

## 🎮 Essayer le jeu

👉 **https://werewolfgame-production.up.railway.app/**

> L'hébergement est sur l'offre gratuite : si le service est en veille, la première ouverture
> peut prendre **~30 à 60 secondes** (réveil du conteneur).

## ✨ Fonctionnalités

- **Connexion rapide** par pseudo + avatar (pas de mot de passe)
- **Salons** : créer un salon (on devient admin) ou en rejoindre un via un **code**
- **Bots** : l'admin peut ajouter des joueurs robots pour compléter la partie
- **Temps réel** : état de la partie, événements et chat poussés via WebSocket (STOMP + SockJS)
- **Attribution automatique des rôles** : Villageois, Loup-Garou, Voyante, Sorcière
- **Phases minutées et configurables** (nuit / jour)
- **Nuit** : les loups votent une victime, la Voyante sonde un rôle, la Sorcière sauve ou empoisonne
- **Jour** : chat de discussion, puis **vote** du village pour éliminer un suspect
- **Fin du vote automatique** dès que tous les joueurs vivants ont voté
- **Conditions de victoire** détectées automatiquement
- **Événements publics et privés** (révélations de rôle, annonces publiques)
- Panneau **Historique** (journal des événements) + ambiance sonore / musique
- **Interface responsive** et plein écran (sans barre de défilement)

## 🧱 Stack technique

| Couche          | Technologie                                                     |
|-----------------|-----------------------------------------------------------------|
| Langage         | Java 17                                                         |
| Framework       | Spring Boot 3.3.2 (Spring MVC, Validation)                      |
| Vue             | Thymeleaf + HTML/CSS/JS natif                                   |
| Temps réel      | Spring WebSocket — STOMP + SockJS                               |
| Persistance     | Spring Data JPA / Hibernate — **PostgreSQL** (H2 en tests)      |
- **Authentification** : session HTTP (pseudo, sans mot de passe)
- **Build** : Maven
- **Conteneurisation** : Dockerfile multi-étapes (JDK 17 → JRE 17)
- **CI** : GitHub Actions
- **Hébergement** : [Railway](https://railway.com) (application) + [Neon](https://neon.tech) (PostgreSQL)

## ⚙️ Prérequis

- Java 17 ou plus récent
- Maven (ou IntelliJ IDEA avec support Maven)
- (optionnel) Docker & Docker Compose pour la base locale

## 🚀 Lancer en local

### Option A — avec Docker (PostgreSQL local, recommandé)

```bash
docker compose up -d      # démarre PostgreSQL sur localhost:5432
mvn spring-boot:run
```

### Option B — avec un PostgreSQL existant

L'application lit la configuration via des variables d'environnement
(valeurs par défaut : `localhost:5432/werewolf`, `postgres` / `postgres`) :

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/werewolf"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="postgres"
mvn spring-boot:run
```

### Ouvrir dans le navigateur

| URL | Description |
|-----|-------------|
| http://localhost:8080 | Application |

> Les tables sont créées automatiquement au premier démarrage (Hibernate `ddl-auto=update`).

## 🕹️ Déroulement d'une partie

1. **Connexion** — saisir un pseudo et choisir un avatar
2. **Salon** — créer un salon (admin) ou en rejoindre un avec un code ; l'admin peut ajouter des bots
3. **Démarrage** — l'admin lance la partie (minimum **5 joueurs**)
4. **Phases de nuit** (minutées) :
   - 🐺 Les **loups** choisissent une victime
   - 🔮 La **voyante** découvre le rôle d'un joueur
   - 🧪 La **sorcière** sauve la victime ou empoisonne un joueur
5. **Phases de jour** (minutées) :
   - 💬 Tous les joueurs discutent dans le **chat**
   - 🗳️ Le village **vote** pour éliminer un suspect (le vote se clôt dès que tout le monde a voté)
6. **Fin de partie** — une condition de victoire est atteinte

## 🎭 Rôles

| Rôle | Camp | Action de nuit |
|------|------|----------------|
| Villageois | Village | Aucune |
| Loup-Garou | Loups | Voter une victime |
| Voyante | Village | Découvrir le rôle d'un joueur |
| Sorcière | Village | Sauver la victime ou empoisonner un joueur |

### Répartition automatique

- Minimum **5 joueurs**
- **1 loup** par tranche de 5 joueurs
- Toujours **1 voyante** et **1 sorcière**
- Les joueurs restants sont **villageois**

## 🏆 Conditions de victoire

- **Le village gagne** lorsque tous les loups sont éliminés
- **Les loups gagnent** lorsqu'ils sont aussi nombreux ou plus que les villageois restants

## ⏱️ Durées des phases

Configurables dans `src/main/resources/application.yml` :

```yaml
werewolf:
  phase:
    wolves: 60   # secondes — choix des loups
    seer: 45     # secondes — voyante
    witch: 60    # secondes — sorcière
    chat: 90     # secondes — discussion
    vote: 45     # secondes — vote du village
```

## 🗺️ Routes principales

| Méthode | URL | Description |
|---------|-----|-------------|
| `GET`  | `/` | Accueil / connexion |
| `POST` | `/join` | Connexion (pseudo + avatar) |
| `GET`  | `/play` | Menu : créer ou rejoindre un salon |
| `POST` | `/room/create` | Créer un salon |
| `POST` | `/room/join` | Rejoindre un salon |
| `GET`  | `/room/{code}` | Salle d'attente (lobby) |
| `POST` | `/room/{code}/addBot` | Ajouter un bot (admin) |
| `POST` | `/room/{code}/start` | Démarrer la partie (admin) |
| `GET`  | `/game/{code}` | Plateau de jeu |
| `POST` | `/game/{code}/wolf-kill` | Action des loups |
| `POST` | `/game/{code}/seer-check` | Action de la voyante |
| `POST` | `/game/{code}/witch-act` | Action de la sorcière |
| `POST` | `/game/{code}/chat` | Envoyer un message |
| `POST` | `/game/{code}/vote` | Voter pour éliminer |

## 🌐 Temps réel (WebSocket)

- **Endpoint** : `/ws` (SockJS)
- **Abonnements (server → client)** :

  | Destination | Contenu |
  |-------------|---------|
  | `/topic/game/{code}/state` | État de la partie (phase, minuteur, vivants) |
  | `/topic/game/{code}/events` | Événements publics |
  | `/topic/game/{code}/chat` | Nouveaux messages de chat |
  | `/topic/room/{code}` | Mises à jour du salon |
  | `/topic/private/{userId}` | Événements privés (voyante, loups…) |

- **Messages (client → server)** : `/app/game/{code}/sync` (demander la synchro de l'état)

> Les actions de jeu passent par les formulaires HTTP (POST); le WebSocket sert à **diffuser**
  l'état et les événements instantanément à tous les joueurs.

## 🗂️ Structure du projet

```
src/main/java/com/example/werewolf/
├── api/                       # DTO + contrôleur STOMP (sync)
│   └── dto/                   # GameStateDto, GameEventDto, ChatMessageDto, RoomUpdateDto
├── config/                    # WebSocket, handshake, durées de phase, conversion DATABASE_URL
├── domain/                    # Entités JPA
│   ├── enums/                 # Role, Phase, GameStatus, NightActionType, EventVisibility
│   ├── Game.java / GamePlayer.java
│   ├── Room.java / RoomMember.java / User.java
│   └── ChatMessage.java / DayVote.java / NightAction.java / GameEvent.java
├── repo/                      # Repositories Spring Data JPA
├── service/                   # GameService, RoomService, AuthService, BotService, WebSocketPublisher
├── ui/                        # Contrôleurs MVC Thymeleaf
│   ├── AuthController.java    # /join, /logout
│   ├── HomeController.java    # /, /play
│   ├── RoomController.java    # /room/**
│   ├── GameController.java    # /game/**
│   └── SimpleErrorController.java
└── WerewolfApplication.java

src/main/resources/
├── templates/                 # index, play, room, game, error (+ layout)
├── static/
│   ├── audio/                 # sons jour/nuit/ambiance
│   ├── avatars/               # avatars des joueurs
│   ├── css/app.css
│   └── js/game.js
├── application.yml            # JPA, Thymeleaf, durées des phases
└── application.properties     # port, datasource, compression, cache
```

## 🐳 Déploiement

Application conteneurisée (Dockerfile multi-étapes) + PostgreSQL managé.

1. **Base de données** : créer une base **PostgreSQL** (ex. [Neon](https://neon.tech)) et récupérer la
   chaîne de connexion `postgresql://...?sslmode=require`.
2. **Plateforme** (ex. [Railway](https://railway.com)) : déployer le dépôt (runtime **Docker**),
   puis définir les variables d'environnement :

   | Variable | Valeur |
   |----------|--------|
   | `SPRING_DATASOURCE_URL` | chaîne de connexion PostgreSQL |
   | `SPRING_DATASOURCE_USERNAME` | utilisateur |
   | `SPRING_DATASOURCE_PASSWORD` | mot de passe |

   > Une variable unique `DATABASE_URL` (chaîne complète) est également acceptée : elle est
   > convertie automatiquement en URL JDBC par `DatabaseUrlEnvironmentPostProcessor`.

Le port est fourni automatiquement par la plateforme (`PORT`) ; en local, l'application écoute
sur `8080`.
