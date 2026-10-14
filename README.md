[![CI](https://github.com/AhlamNasri/werewolf_game/actions/workflows/ci.yml/badge.svg)](https://github.com/AhlamNasri/werewolf_game/actions/workflows/ci.yml)

# Loup-Garou (Werewolf)

A multiplayer Werewolf party game built with Spring Boot and Thymeleaf, featuring real-time polling, role-based night actions, a day vote phase, and an in-game chat.

## Tech Stack

- **Backend:** Java 17 + Spring Boot 3.3.2
- **Templating:** Thymeleaf
- **Database:** H2 
- **ORM:** Spring Data JPA / Hibernate
- **Auth:** Session-based + BCrypt password hashing
- **Build:** Maven

## Features

- User registration and login
- Create a room (as admin) or join one with a room code
- Minimum 5 players required to start
- Automatic role assignment: Villager, Wolf, Seer, Witch
- Timed game phases with configurable durations
- Night phase: Wolves vote to eliminate, Seer investigates, Witch uses potions
- Day phase: Group chat, then vote to eliminate a suspect
- Win condition detection: Village wins when all wolves are eliminated; Wolves win when they equal or outnumber the villagers
- Per-player event visibility (private role reveals, public announcements)
- H2 console for database inspection during development

## Prerequisites

- Java 17 or higher (Java 21 is fine)
- Maven (or IntelliJ IDEA with Maven support)

## Getting Started

### 1. Clone the repository

```bash
git clone <repo-url>
cd werewolf
```

### 2. Run the application

```bash
mvn spring-boot:run
```

Or in IntelliJ: open the project and run `WerewolfApplication`.

### 3. Open in your browser

| URL | Description |
|-----|-------------|
| http://localhost:8080 | Application |
| http://localhost:8080/h2 | H2 database console |

**H2 console credentials:**

```
JDBC URL:  jdbc:h2:file:./data/werewolf
Username:  sa
Password:  (leave blank)
```

## Game Flow

1. **Register / Log in**
2. **Lobby** — Create a room (you become admin) or join one using a room code
3. **Start** — Admin starts the game once at least 5 players have joined
4. **Night phases** (timed):
   - Wolves vote on a target
   - Seer investigates one player's role
   - Witch uses save or poison potion
5. **Day phases** (timed):
   - All players discuss in chat
   - Players vote to eliminate a suspect
6. **Win condition** — game ends when:
   - All wolves are eliminated → **Village wins**
   - Wolves equal or outnumber remaining villagers → **Wolves win**

## Roles

| Role | Faction | Night action |
|------|---------|--------------|
| Villager | Village | None |
| Wolf | Wolves | Vote to eliminate a player |
| Seer | Village | Reveal one player's role |
| Witch | Village | Save the night's victim or poison a player |

## Phase Durations

Configurable in `src/main/resources/application.yml`:

```yaml
werewolf:
  phase:
    wolves: 60   # seconds
    seer: 45
    witch: 60
    chat: 90
    vote: 45
```

## Project Structure

```
src/main/java/com/example/werewolf/
├── api/                  # REST endpoints (game state polling, chat, actions)
│   └── dto/              # GameStateDto, GameEventDto, ChatMessageDto
├── config/               # PhaseDurationsProperties
├── domain/               # JPA entities
│   ├── enums/            # Role, Phase, GameStatus, NightActionType, EventVisibility
│   ├── Game.java
│   ├── GamePlayer.java
│   ├── Room.java
│   ├── RoomMember.java
│   ├── User.java
│   ├── ChatMessage.java
│   ├── DayVote.java
│   ├── NightAction.java
│   └── GameEvent.java
├── repo/                 # Spring Data JPA repositories
├── service/              # GameService, RoomService, AuthService
├── ui/                   # Thymeleaf MVC controllers
│   ├── AuthController.java
│   ├── HomeController.java
│   ├── RoomController.java
│   └── GameController.java
└── WerewolfApplication.java

src/main/resources/
├── templates/            # Thymeleaf HTML templates
├── static/
│   ├── audio/            # Day/night/ambient sound effects
│   ├── avatars/          # Player avatar images
│   ├── css/app.css
│   └── js/game.js
└── application.yml
```
