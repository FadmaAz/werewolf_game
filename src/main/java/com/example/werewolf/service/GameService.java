package com.example.werewolf.service;

import com.example.werewolf.config.PhaseDurationsProperties;
import com.example.werewolf.domain.*;
import com.example.werewolf.domain.enums.*;
import com.example.werewolf.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GameService {

  private final RoomRepository roomRepo;
  private final RoomMemberRepository memberRepo;
  private final GameRepository gameRepo;
  private final GamePlayerRepository gpRepo;
  private final NightActionRepository nightRepo;
  private final DayVoteRepository voteRepo;
  private final ChatMessageRepository chatRepo;
  private final GameEventRepository eventRepo;
  private final PhaseDurationsProperties durations;

  private final SecureRandom rnd = new SecureRandom();

  public GameService(RoomRepository roomRepo,
                     RoomMemberRepository memberRepo,
                     GameRepository gameRepo,
                     GamePlayerRepository gpRepo,
                     NightActionRepository nightRepo,
                     DayVoteRepository voteRepo,
                     ChatMessageRepository chatRepo,
                     GameEventRepository eventRepo,
                     PhaseDurationsProperties durations) {
    this.roomRepo = roomRepo;
    this.memberRepo = memberRepo;
    this.gameRepo = gameRepo;
    this.gpRepo = gpRepo;
    this.nightRepo = nightRepo;
    this.voteRepo = voteRepo;
    this.chatRepo = chatRepo;
    this.eventRepo = eventRepo;
    this.durations = durations;
  }

  // ---------------------- Room / Game access ----------------------

  public Room requireRoom(String code) {
    return roomRepo.findByCode(code)
      .orElseThrow(() -> new IllegalArgumentException("Room introuvable."));
  }

  public Optional<Game> findGameByRoomCode(String roomCode) {
    return gameRepo.findByRoom_Code(roomCode);
  }

  public Game requireGame(String roomCode) {
    return gameRepo.findByRoom_Code(roomCode)
      .orElseThrow(() -> new IllegalArgumentException("Partie introuvable (pas encore démarrée)."));
  }

  public GamePlayer requireGamePlayer(Game game, User user) {
    return gpRepo.findByGameAndUser(game, user)
      .orElseThrow(() -> new IllegalArgumentException("Vous n'êtes pas dans cette partie."));
  }

  public boolean isAdmin(Room room, User user) {
    return room.getAdmin().getId().equals(user.getId());
  }

  // ---------------------- Start game ----------------------

  @Transactional
  public Game startGame(String roomCode, User admin) {
    Room room = requireRoom(roomCode);
    if (!isAdmin(room, admin)) throw new IllegalArgumentException("Seul l'admin peut démarrer.");
    if (room.isStarted()) throw new IllegalArgumentException("La partie a déjà démarré.");

    long count = memberRepo.countByRoom(room);
    if (count < 5) throw new IllegalArgumentException("Minimum 5 joueurs pour démarrer.");

    // create game
    Game game = new Game();
    game.setRoom(room);
    game.setStatus(GameStatus.RUNNING);
    game.setPhase(Phase.NIGHT_WOLVES);
    game.setDayNumber(1);
    game.setPhaseEndsAt(Instant.now().plusSeconds(durations.getWolves()));
    game = gameRepo.save(game);

    // create game players from room members
    List<RoomMember> members = memberRepo.findByRoomOrderByJoinedAtAsc(room);
    List<User> users = members.stream().map(RoomMember::getUser).toList();

    List<Role> roles = buildRoles(users.size());
    Collections.shuffle(roles, rnd);

    List<GamePlayer> gamePlayers = new ArrayList<>();
    for (int i = 0; i < users.size(); i++) {
      GamePlayer gp = new GamePlayer();
      gp.setGame(game);
      gp.setUser(users.get(i));
      gp.setRole(roles.get(i));
      gp.setAlive(true);
      if (roles.get(i) == Role.WITCH) {
        gp.setWitchHealAvailable(true);
        gp.setWitchPoisonAvailable(true);
      }
      gamePlayers.add(gp);
    }
    gpRepo.saveAll(gamePlayers);

    room.setStarted(true);
    roomRepo.save(room);

    addPublicEvent(game, game.getDayNumber(), "🌙 La nuit tombe... Les loups se réveillent.");

    // wolves know other wolves
    revealWolvesToWolves(game);

    return game;
  }

  private List<Role> buildRoles(int playerCount) {
    // Rules:
    // - Minimum 5 players.
    // - For each group of 5 players: 1 wolf.
    // - Always 1 seer and 1 witch (for >=5 players).
    // - Remaining are villagers.
    int wolves = Math.max(1, playerCount / 5);
    int seer = 1;
    int witch = 1;
    int fixed = wolves + seer + witch;
    if (fixed > playerCount) {
      // fallback
      wolves = 1;
      seer = 1;
      witch = 0;
      fixed = wolves + seer + witch;
    }
    int villagers = playerCount - fixed;

    List<Role> roles = new ArrayList<>();
    for (int i = 0; i < wolves; i++) roles.add(Role.WOLF);
    for (int i = 0; i < seer; i++) roles.add(Role.SEER);
    for (int i = 0; i < witch; i++) roles.add(Role.WITCH);
    for (int i = 0; i < villagers; i++) roles.add(Role.VILLAGER);
    return roles;
  }

  // ---------------------- Tick (timers) ----------------------

  @Transactional
  public Game tickIfNeeded(Game game) {
    if (game.getStatus() != GameStatus.RUNNING) return game;

    Instant now = Instant.now();
    if (game.getPhaseEndsAt() != null && now.isAfter(game.getPhaseEndsAt())) {
      // phase timeout -> advance
      switch (game.getPhase()) {
        case NIGHT_WOLVES -> advanceFromWolves(game, true);
        case NIGHT_SEER -> advanceFromSeer(game, true);
        case NIGHT_WITCH -> advanceFromWitch(game, true);
        case DAY_CHAT -> advanceFromChat(game);
        case DAY_VOTE -> advanceFromVote(game);
        default -> {}
      }
    }
    return game;
  }

  // ---------------------- Night actions ----------------------

  @Transactional
  public void wolfKill(String roomCode, User user, long targetGamePlayerId) {
    Game game = requireGame(roomCode);
    tickIfNeeded(game);
    if (game.getPhase() != Phase.NIGHT_WOLVES) throw new IllegalArgumentException("Ce n'est pas le tour des loups.");

    GamePlayer actor = requireGamePlayer(game, user);
    if (!actor.isAlive()) throw new IllegalArgumentException("Vous êtes mort.");
    if (actor.getRole() != Role.WOLF) throw new IllegalArgumentException("Seuls les loups peuvent agir.");

    GamePlayer target = gpRepo.findById(targetGamePlayerId).orElseThrow(() -> new IllegalArgumentException("Cible introuvable."));
    if (!target.getGame().getId().equals(game.getId())) throw new IllegalArgumentException("Cible invalide.");
    if (!target.isAlive()) throw new IllegalArgumentException("Cible déjà morte.");
    if (target.getUser().getId().equals(user.getId())) throw new IllegalArgumentException("Vous ne pouvez pas vous choisir.");
    if (target.getRole() == Role.WOLF) throw new IllegalArgumentException("Les loups ne peuvent pas tuer un loup.");

    int day = game.getDayNumber();
    NightAction existing = nightRepo.findByGameAndDayNumberAndActorAndType(game, day, actor, NightActionType.WOLF_KILL).orElse(null);

    // Build current chosen targets by other wolves
    List<NightAction> allWolfKills = nightRepo.findByGameAndDayNumberAndType(game, day, NightActionType.WOLF_KILL);
    Set<Long> chosenTargetIdsByOthers = allWolfKills.stream()
      .filter(a -> !a.getActor().getId().equals(actor.getId()))
      .map(a -> a.getTarget().getId())
      .collect(Collectors.toSet());

    if (chosenTargetIdsByOthers.contains(target.getId())) {
      throw new IllegalArgumentException("Cette cible est déjà choisie par un autre loup. Choisissez quelqu'un d'autre.");
    }

    if (existing == null) {
      NightAction a = new NightAction();
      a.setGame(game);
      a.setDayNumber(day);
      a.setActor(actor);
      a.setType(NightActionType.WOLF_KILL);
      a.setTarget(target);
      nightRepo.save(a);
    } else {
      existing.setTarget(target);
      nightRepo.save(existing);
    }

    addPrivateEvent(game, day, user, "✅ Vous avez choisi: " + target.getUser().getUsername());

    // if all wolves have acted => advance
    if (allWolvesActed(game)) {
      advanceFromWolves(game, false);
    }
  }

  private boolean allWolvesActed(Game game) {
    int day = game.getDayNumber();
    List<GamePlayer> wolves = gpRepo.findByGameAndAliveTrueAndRole(game, Role.WOLF);
    if (wolves.isEmpty()) return true;
    List<NightAction> actions = nightRepo.findByGameAndDayNumberAndType(game, day, NightActionType.WOLF_KILL);
    Set<Long> acted = actions.stream().map(a -> a.getActor().getId()).collect(Collectors.toSet());
    return wolves.stream().allMatch(w -> acted.contains(w.getId()));
  }


private void autoPickMissingWolfTargets(Game game) {
  int day = game.getDayNumber();
  List<GamePlayer> wolves = gpRepo.findByGameAndAliveTrueAndRole(game, Role.WOLF);
  if (wolves.isEmpty()) return;

  List<NightAction> existingKills = nightRepo.findByGameAndDayNumberAndType(game, day, NightActionType.WOLF_KILL);
  Set<Long> actedWolfIds = existingKills.stream().map(a -> a.getActor().getId()).collect(Collectors.toSet());
  Set<Long> chosenTargets = existingKills.stream().map(a -> a.getTarget().getId()).collect(Collectors.toSet());

  List<GamePlayer> aliveNonWolves = gpRepo.findByGameOrderByIdAsc(game).stream()
    .filter(p -> p.isAlive() && p.getRole() != Role.WOLF)
    .toList();

  // available unique targets
  List<GamePlayer> available = aliveNonWolves.stream()
    .filter(p -> !chosenTargets.contains(p.getId()))
    .collect(Collectors.toCollection(ArrayList::new));

  for (GamePlayer wolf : wolves) {
    if (actedWolfIds.contains(wolf.getId())) continue;

    GamePlayer target = null;
    if (!available.isEmpty()) {
      target = available.remove(rnd.nextInt(available.size()));
    } else if (!aliveNonWolves.isEmpty()) {
      // fallback (should rarely happen)
      target = aliveNonWolves.get(rnd.nextInt(aliveNonWolves.size()));
    }
    if (target == null) continue;

    NightAction a = new NightAction();
    a.setGame(game);
    a.setDayNumber(day);
    a.setActor(wolf);
    a.setType(NightActionType.WOLF_KILL);
    a.setTarget(target);
    nightRepo.save(a);

    addPrivateEvent(game, day, wolf.getUser(), "⏱️ Temps écoulé: cible choisie automatiquement: " + target.getUser().getUsername());
    chosenTargets.add(target.getId());
  }
}

  @Transactional
  public void seerCheck(String roomCode, User user, long targetGamePlayerId) {
    Game game = requireGame(roomCode);
    tickIfNeeded(game);
    if (game.getPhase() != Phase.NIGHT_SEER) throw new IllegalArgumentException("Ce n'est pas le tour de la voyante.");

    GamePlayer actor = requireGamePlayer(game, user);
    if (!actor.isAlive()) throw new IllegalArgumentException("Vous êtes mort.");
    if (actor.getRole() != Role.SEER) throw new IllegalArgumentException("Seule la voyante peut agir.");

    GamePlayer target = gpRepo.findById(targetGamePlayerId).orElseThrow(() -> new IllegalArgumentException("Cible introuvable."));
    if (!target.getGame().getId().equals(game.getId())) throw new IllegalArgumentException("Cible invalide.");
    if (!target.isAlive()) throw new IllegalArgumentException("Cible déjà morte.");
    if (target.getUser().getId().equals(user.getId())) throw new IllegalArgumentException("Vous ne pouvez pas vous choisir.");

    int day = game.getDayNumber();

    NightAction existing = nightRepo.findByGameAndDayNumberAndActorAndType(game, day, actor, NightActionType.SEER_CHECK).orElse(null);
    if (existing == null) {
      NightAction a = new NightAction();
      a.setGame(game);
      a.setDayNumber(day);
      a.setActor(actor);
      a.setType(NightActionType.SEER_CHECK);
      a.setTarget(target);
      nightRepo.save(a);
    } else {
      existing.setTarget(target);
      nightRepo.save(existing);
    }

    addPrivateEvent(game, day, user, "🔮 Voyante: " + target.getUser().getUsername() + " est " + target.getRole());

    advanceFromSeer(game, false);
  }

  @Transactional
  public void witchAct(String roomCode, User user, Long healTargetId, Long poisonTargetId) {
    Game game = requireGame(roomCode);
    tickIfNeeded(game);
    if (game.getPhase() != Phase.NIGHT_WITCH) throw new IllegalArgumentException("Ce n'est pas le tour de la sorcière.");

    GamePlayer witch = requireGamePlayer(game, user);
    if (!witch.isAlive()) throw new IllegalArgumentException("Vous êtes mort.");
    if (witch.getRole() != Role.WITCH) throw new IllegalArgumentException("Seule la sorcière peut agir.");

    int day = game.getDayNumber();

    // heal: must be among wolf targets
    if (healTargetId != null) {
      if (!witch.isWitchHealAvailable()) throw new IllegalArgumentException("Potion de soin déjà utilisée.");
      GamePlayer healTarget = gpRepo.findById(healTargetId)
        .orElseThrow(() -> new IllegalArgumentException("Cible soin introuvable."));
      if (!healTarget.getGame().getId().equals(game.getId())) throw new IllegalArgumentException("Cible soin invalide.");
      if (!healTarget.isAlive()) throw new IllegalArgumentException("Cible soin déjà morte.");

      Set<Long> wolfTargets = nightRepo.findByGameAndDayNumberAndType(game, day, NightActionType.WOLF_KILL)
        .stream().map(a -> a.getTarget().getId()).collect(Collectors.toSet());

      if (!wolfTargets.contains(healTarget.getId())) {
        throw new IllegalArgumentException("Vous ne pouvez soigner que la/les victime(s) attaquée(s) par les loups.");
      }

      // consume potion immediately (simpler)
      witch.setWitchHealAvailable(false);
      gpRepo.save(witch);

      NightAction a = nightRepo.findByGameAndDayNumberAndActorAndType(game, day, witch, NightActionType.WITCH_HEAL).orElseGet(NightAction::new);
      a.setGame(game);
      a.setDayNumber(day);
      a.setActor(witch);
      a.setType(NightActionType.WITCH_HEAL);
      a.setTarget(healTarget);
      nightRepo.save(a);

      addPrivateEvent(game, day, user, "🧪 Potion de soin utilisée sur: " + healTarget.getUser().getUsername());
    }

    if (poisonTargetId != null) {
      if (!witch.isWitchPoisonAvailable()) throw new IllegalArgumentException("Potion de poison déjà utilisée.");
      GamePlayer poisonTarget = gpRepo.findById(poisonTargetId)
        .orElseThrow(() -> new IllegalArgumentException("Cible poison introuvable."));
      if (!poisonTarget.getGame().getId().equals(game.getId())) throw new IllegalArgumentException("Cible poison invalide.");
      if (!poisonTarget.isAlive()) throw new IllegalArgumentException("Cible poison déjà morte.");
      if (poisonTarget.getUser().getId().equals(user.getId())) throw new IllegalArgumentException("Vous ne pouvez pas vous empoisonner.");

      witch.setWitchPoisonAvailable(false);
      gpRepo.save(witch);

      NightAction a = nightRepo.findByGameAndDayNumberAndActorAndType(game, day, witch, NightActionType.WITCH_POISON).orElseGet(NightAction::new);
      a.setGame(game);
      a.setDayNumber(day);
      a.setActor(witch);
      a.setType(NightActionType.WITCH_POISON);
      a.setTarget(poisonTarget);
      nightRepo.save(a);

      addPrivateEvent(game, day, user, "☠️ Potion de poison utilisée sur: " + poisonTarget.getUser().getUsername());
    }

    advanceFromWitch(game, false);
  }

  // ---------------------- Advance phases ----------------------

  private void advanceFromWolves(Game game, boolean timeout) {
    // wolves -> seer or witch or resolve
    if (game.getPhase() != Phase.NIGHT_WOLVES) return;

    if (timeout) {
      // If time is over, auto-pick targets for wolves who didn't act (unique if possible)
      autoPickMissingWolfTargets(game);
    }


    // if no alive seer, skip
    boolean seerAlive = gpRepo.findByGameAndAliveTrueAndRole(game, Role.SEER).size() > 0;
    boolean witchAlive = gpRepo.findByGameAndAliveTrueAndRole(game, Role.WITCH).size() > 0;

    if (seerAlive) {
      game.setPhase(Phase.NIGHT_SEER);
      game.setPhaseEndsAt(Instant.now().plusSeconds(durations.getSeer()));
      gameRepo.save(game);
      addPublicEvent(game, game.getDayNumber(), "✨ La voyante se réveille...");
      return;
    }

    if (witchAlive) {
      game.setPhase(Phase.NIGHT_WITCH);
      game.setPhaseEndsAt(Instant.now().plusSeconds(durations.getWitch()));
      gameRepo.save(game);
      addPublicEvent(game, game.getDayNumber(), "🧙‍♀️ La sorcière se réveille...");
      return;
    }

    resolveNightAndStartDay(game);
  }

  private void advanceFromSeer(Game game, boolean timeout) {
    if (game.getPhase() != Phase.NIGHT_SEER) return;

    boolean witchAlive = gpRepo.findByGameAndAliveTrueAndRole(game, Role.WITCH).size() > 0;
    if (witchAlive) {
      game.setPhase(Phase.NIGHT_WITCH);
      game.setPhaseEndsAt(Instant.now().plusSeconds(durations.getWitch()));
      gameRepo.save(game);
      addPublicEvent(game, game.getDayNumber(), "🧙‍♀️ La sorcière se réveille...");
      return;
    }
    resolveNightAndStartDay(game);
  }

  private void advanceFromWitch(Game game, boolean timeout) {
    if (game.getPhase() != Phase.NIGHT_WITCH) return;
    resolveNightAndStartDay(game);
  }

  private void advanceFromChat(Game game) {
    if (game.getPhase() != Phase.DAY_CHAT) return;
    game.setPhase(Phase.DAY_VOTE);
    game.setPhaseEndsAt(Instant.now().plusSeconds(durations.getVote()));
    gameRepo.save(game);
    addPublicEvent(game, game.getDayNumber(), "🗳️ Vote du village ! Choisissez qui éliminer.");
  }

  private void advanceFromVote(Game game) {
    if (game.getPhase() != Phase.DAY_VOTE) return;
    resolveDayVoteAndStartNight(game);
  }

  // ---------------------- Resolve night/day ----------------------

  private void resolveNightAndStartDay(Game game) {
    int day = game.getDayNumber();

    List<NightAction> wolfKills = nightRepo.findByGameAndDayNumberAndType(game, day, NightActionType.WOLF_KILL);
    Set<Long> killTargetIds = wolfKills.stream()
      .map(a -> a.getTarget().getId())
      .collect(Collectors.toSet());

    // heal
    List<NightAction> heals = nightRepo.findByGameAndDayNumberAndType(game, day, NightActionType.WITCH_HEAL);
    if (!heals.isEmpty() && heals.get(0).getTarget() != null) {
      killTargetIds.remove(heals.get(0).getTarget().getId());
    }

    // poison
    List<NightAction> poisons = nightRepo.findByGameAndDayNumberAndType(game, day, NightActionType.WITCH_POISON);
    if (!poisons.isEmpty() && poisons.get(0).getTarget() != null) {
      killTargetIds.add(poisons.get(0).getTarget().getId());
    }

    List<GamePlayer> killed = new ArrayList<>();
    for (Long id : killTargetIds) {
      GamePlayer gp = gpRepo.findById(id).orElse(null);
      if (gp != null && gp.isAlive()) {
        gp.setAlive(false);
        gp.setEliminatedAt(Instant.now());
        killed.add(gp);
      }
    }
    gpRepo.saveAll(killed);

    if (killed.isEmpty()) {
      addPublicEvent(game, day, "🌅 Matin: Personne n'est mort cette nuit.");
    } else {
      for (GamePlayer gp : killed) {
        addPublicEvent(game, day, "🌅 Matin: " + gp.getUser().getUsername() + " est mort.");
      }
    }

    // Check win
    if (checkAndFinishIfNeeded(game, day)) return;

    // Start day chat
    game.setPhase(Phase.DAY_CHAT);
    game.setPhaseEndsAt(Instant.now().plusSeconds(durations.getChat()));
    gameRepo.save(game);
    addPublicEvent(game, day, "☀️ Discussion (chat) - Jour " + day + ".");
  }

  private void resolveDayVoteAndStartNight(Game game) {
    int day = game.getDayNumber();

    List<DayVote> votes = voteRepo.findByGameAndDayNumber(game, day);
    Map<Long, Long> counts = votes.stream()
      .collect(Collectors.groupingBy(v -> v.getTarget().getId(), Collectors.counting()));

    GamePlayer eliminated = null;
    if (!counts.isEmpty()) {
      long max = counts.values().stream().mapToLong(x -> x).max().orElse(0);
      List<Long> top = counts.entrySet().stream()
        .filter(e -> e.getValue() == max)
        .map(Map.Entry::getKey).toList();

      if (top.size() == 1) {
        eliminated = gpRepo.findById(top.get(0)).orElse(null);
      }
    }

    if (eliminated == null) {
      addPublicEvent(game, day, "🗳️ Résultat du vote: égalité ou aucun vote. Personne n'est éliminé.");
    } else {
      if (eliminated.isAlive()) {
        eliminated.setAlive(false);
        eliminated.setEliminatedAt(Instant.now());
        gpRepo.save(eliminated);
      }
      addPublicEvent(game, day, "🗳️ Résultat du vote: " + eliminated.getUser().getUsername() + " est éliminé.");
    }

    if (checkAndFinishIfNeeded(game, day)) return;

    // Start next night
    game.setPhase(Phase.NIGHT_WOLVES);
    game.setDayNumber(day + 1);
    game.setPhaseEndsAt(Instant.now().plusSeconds(durations.getWolves()));
    gameRepo.save(game);
    addPublicEvent(game, game.getDayNumber(), "🌙 Nuit " + game.getDayNumber() + ": les loups se réveillent.");

    revealWolvesToWolves(game);

    // Cleanup old votes/actions not necessary, but keep DB small:
    // votes/actions are per dayNumber, no need to delete; can keep history.
  }

  private boolean checkAndFinishIfNeeded(Game game, int day) {
    long wolvesAlive = gpRepo.countByGameAndAliveTrueAndRole(game, Role.WOLF);
    long alive = gpRepo.countByGameAndAliveTrue(game);
    long othersAlive = alive - wolvesAlive;

    if (wolvesAlive <= 0) {
      finishGame(game, "VILLAGE", day);
      return true;
    }
    if (wolvesAlive >= othersAlive) {
      finishGame(game, "WOLVES", day);
      return true;
    }
    return false;
  }

  private void finishGame(Game game, String winner, int day) {
    game.setStatus(GameStatus.FINISHED);
    game.setPhase(Phase.FINISHED);
    game.setWinner(winner);
    game.setPhaseEndsAt(null);
    gameRepo.save(game);

    if ("VILLAGE".equals(winner)) addPublicEvent(game, day, "🏆 Victoire du Village !");
    if ("WOLVES".equals(winner)) addPublicEvent(game, day, "🏆 Victoire des Loups !");

    // reveal roles
    List<GamePlayer> players = gpRepo.findByGameOrderByIdAsc(game);
    for (GamePlayer p : players) {
      addPublicEvent(game, day, "Rôle: " + p.getUser().getUsername() + " = " + p.getRole());
    }
  }

  // ---------------------- Day chat/vote ----------------------

  @Transactional
  public void postChat(String roomCode, User user, String message) {
    Game game = requireGame(roomCode);
    tickIfNeeded(game);
    if (game.getPhase() != Phase.DAY_CHAT) throw new IllegalArgumentException("Le chat est fermé (pas en phase discussion).");

    GamePlayer gp = requireGamePlayer(game, user);
    if (!gp.isAlive()) throw new IllegalArgumentException("Vous êtes mort, vous ne pouvez plus discuter.");

    String m = (message == null) ? "" : message.trim();
    if (m.isBlank()) return;
    if (m.length() > 300) throw new IllegalArgumentException("Message trop long (max 300).");

    ChatMessage cm = new ChatMessage();
    cm.setGame(game);
    cm.setDayNumber(game.getDayNumber());
    cm.setSender(user);
    cm.setMessage(m);
    chatRepo.save(cm);
  }

  @Transactional
  public void vote(String roomCode, User user, long targetGamePlayerId) {
    Game game = requireGame(roomCode);
    tickIfNeeded(game);
    if (game.getPhase() != Phase.DAY_VOTE) throw new IllegalArgumentException("Ce n'est pas le moment de voter.");

    GamePlayer voter = requireGamePlayer(game, user);
    if (!voter.isAlive()) throw new IllegalArgumentException("Vous êtes mort, vous ne pouvez plus voter.");

    GamePlayer target = gpRepo.findById(targetGamePlayerId).orElseThrow(() -> new IllegalArgumentException("Cible introuvable."));
    if (!target.getGame().getId().equals(game.getId())) throw new IllegalArgumentException("Cible invalide.");
    if (!target.isAlive()) throw new IllegalArgumentException("Cible déjà morte.");
    if (target.getUser().getId().equals(user.getId())) throw new IllegalArgumentException("Vote invalide.");

    int day = game.getDayNumber();
    DayVote v = voteRepo.findByGameAndDayNumberAndVoter(game, day, voter).orElseGet(DayVote::new);
    v.setGame(game);
    v.setDayNumber(day);
    v.setVoter(voter);
    v.setTarget(target);
    voteRepo.save(v);
  }

  // ---------------------- Events helpers ----------------------

  private void addPublicEvent(Game game, int day, String msg) {
    GameEvent e = new GameEvent();
    e.setGame(game);
    e.setDayNumber(day);
    e.setVisibility(EventVisibility.PUBLIC);
    e.setMessage(msg);
    eventRepo.save(e);
  }

  private void addPrivateEvent(Game game, int day, User recipient, String msg) {
    GameEvent e = new GameEvent();
    e.setGame(game);
    e.setDayNumber(day);
    e.setVisibility(EventVisibility.PRIVATE);
    e.setRecipient(recipient);
    e.setMessage(msg);
    eventRepo.save(e);
  }

  private void revealWolvesToWolves(Game game) {
    List<GamePlayer> wolves = gpRepo.findByGameAndAliveTrueAndRole(game, Role.WOLF);
    if (wolves.size() <= 1) return;
    String msg = "🐺 Les loups sont: " + wolves.stream().map(w -> w.getUser().getUsername()).collect(Collectors.joining(", "));
    for (GamePlayer w : wolves) {
      addPrivateEvent(game, game.getDayNumber(), w.getUser(), msg);
    }
  }

  // ---------------------- Helpers for UI ----------------------

  public Set<Long> getWolfChosenTargets(Game game) {
    int day = game.getDayNumber();
    return nightRepo.findByGameAndDayNumberAndType(game, day, NightActionType.WOLF_KILL)
      .stream().map(a -> a.getTarget().getId()).collect(Collectors.toSet());
  }

  public Optional<Long> getMyWolfTarget(Game game, GamePlayer me) {
    int day = game.getDayNumber();
    return nightRepo.findByGameAndDayNumberAndActorAndType(game, day, me, NightActionType.WOLF_KILL)
      .map(a -> a.getTarget().getId());
  }

  public List<GameEvent> getVisibleEvents(Game game, User me, long afterId) {
    List<GameEvent> pub = eventRepo.findByGameAndVisibilityAndIdGreaterThanOrderByIdAsc(game, EventVisibility.PUBLIC, afterId);
    List<GameEvent> priv = eventRepo.findByGameAndRecipientAndVisibilityAndIdGreaterThanOrderByIdAsc(game, me, EventVisibility.PRIVATE, afterId);
    List<GameEvent> all = new ArrayList<>();
    all.addAll(pub);
    all.addAll(priv);
    all.sort(Comparator.comparing(GameEvent::getId));
    return all;
  }

  public List<ChatMessage> getChat(Game game, long afterId) {
    return chatRepo.findByGameAndIdGreaterThanOrderByIdAsc(game, afterId);
  }
}