package com.example.werewolf.service;

import com.example.werewolf.api.dto.RoomUpdateDto;
import com.example.werewolf.domain.*;
import com.example.werewolf.domain.enums.*;
import com.example.werewolf.repo.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

/**
 * BotService — manages robot players.
 * Bots are regular User entities with isBot=true.
 * Their actions are triggered every 4 seconds by @Scheduled.
 */
@Service
public class BotService {

  private static final int MAX_BOTS = 5;

  private static final List<String> BOT_NAMES = List.of(
    "🤖 Loup-Bot", "🤖 VillageBot", "🤖 SageBot", "🤖 OmbreBot", "🤖 MystBot"
  );

  private static final List<String> BOT_CHAT_MESSAGES = List.of(
    "🤔 Je ne sais pas qui suspecter...",
    "Quelqu'un se comporte bizarrement !",
    "J'ai un mauvais pressentiment ce matin.",
    "On doit rester unis, villageois !",
    "Je vote pour celui qui parle le moins.",
    "Les loups sont parmi nous, j'en suis certain !",
    "Méfions-nous des silencieux...",
    "Qui a voté pour moi hier ? 😤",
    "Personne ne me semble suspect en fait.",
    "Faites confiance à votre instinct !"
  );

  private final UserRepository userRepo;
  private final RoomRepository roomRepo;
  private final RoomMemberRepository memberRepo;
  private final GameRepository gameRepo;
  private final GamePlayerRepository gpRepo;
  private final NightActionRepository nightRepo;
  private final DayVoteRepository voteRepo;
  private final ChatMessageRepository chatRepo;
  private final GameService gameService;
  private final WebSocketPublisher publisher;

  private final SecureRandom rnd = new SecureRandom();

  // Track last bot chat time per game to avoid spam
  private final Map<Long, Long> lastBotChatTime = new HashMap<>();

  public BotService(UserRepository userRepo,
                    RoomRepository roomRepo,
                    RoomMemberRepository memberRepo,
                    GameRepository gameRepo,
                    GamePlayerRepository gpRepo,
                    NightActionRepository nightRepo,
                    DayVoteRepository voteRepo,
                    ChatMessageRepository chatRepo,
                    GameService gameService,
                    WebSocketPublisher publisher) {
    this.userRepo = userRepo;
    this.roomRepo = roomRepo;
    this.memberRepo = memberRepo;
    this.gameRepo = gameRepo;
    this.gpRepo = gpRepo;
    this.nightRepo = nightRepo;
    this.voteRepo = voteRepo;
    this.chatRepo = chatRepo;
    this.gameService = gameService;
    this.publisher = publisher;
  }

  // ---------------------- Add bot to room ----------------------

  @Transactional
  public void addBotToRoom(String roomCode, User admin) {
    Room room = roomRepo.findByCode(roomCode)
      .orElseThrow(() -> new IllegalArgumentException("Room introuvable."));

    if (!room.getAdmin().getId().equals(admin.getId())) {
      throw new IllegalArgumentException("Seul l'admin peut ajouter des bots.");
    }
    if (room.isStarted()) {
      throw new IllegalArgumentException("La partie a déjà démarré.");
    }

    // Count existing bots in room
    List<RoomMember> members = memberRepo.findByRoomOrderByJoinedAtAsc(room);
    long botCount = members.stream().filter(m -> m.getUser().isBot()).count();
    if (botCount >= MAX_BOTS) {
      throw new IllegalArgumentException("Maximum " + MAX_BOTS + " bots autorisés par salon.");
    }

    // Pick a bot name not already taken in this room
    Set<String> usedNames = members.stream()
      .map(m -> m.getUser().getUsername())
      .collect(Collectors.toSet());

    String botName = BOT_NAMES.stream()
      .filter(n -> !usedNames.contains(n))
      .findFirst()
      .orElse("🤖 Bot-" + (botCount + 1));

    // Find or create bot user
    User bot = userRepo.findByUsernameIgnoreCase(botName).orElseGet(() -> {
      User u = new User();
      u.setUsername(botName);
      u.setAvatar("avatar" + (rnd.nextInt(10) + 1) + ".png");
      u.setBot(true);
      return userRepo.save(u);
    });

    // Add to room if not already member
    if (memberRepo.findByRoomAndUser(room, bot).isEmpty()) {
      RoomMember rm = new RoomMember();
      rm.setRoom(room);
      rm.setUser(bot);
      memberRepo.save(rm);
    }

    RoomUpdateDto update = new RoomUpdateDto();
    update.roomCode = room.getCode();
    update.type = "ROOM_UPDATE";
    update.playerCount = (int) memberRepo.countByRoom(room);
    update.started = room.isStarted();
    publisher.publishRoomUpdate(update);
  }

  // ---------------------- Scheduled bot actions ----------------------

  /**
   * Every 4 seconds, process all active games and make bots act.
   * A small random delay prevents all bots acting at the exact same instant.
   */
  @Scheduled(fixedDelay = 4000)
  @Transactional
  public void executeBotActions() {
    List<Game> activeGames = gameRepo.findAll().stream()
      .filter(g -> g.getStatus() == GameStatus.RUNNING)
      .toList();

    for (Game game : activeGames) {
      try {
        // Trigger tick first to advance phase if time expired
        gameService.tickIfNeeded(game);
        // Reload after tick
        Game refreshed = gameRepo.findById(game.getId()).orElse(game);
        if (refreshed.getStatus() != GameStatus.RUNNING) continue;

        processBotActionsForGame(refreshed);
      } catch (Exception ignored) {
        // Don't let one game failure break others
      }
    }
  }

  private void processBotActionsForGame(Game game) {
    Phase phase = game.getPhase();
    List<GamePlayer> botPlayers = gpRepo.findByGameOrderByIdAsc(game).stream()
      .filter(gp -> gp.isAlive() && gp.getUser().isBot())
      .toList();

    if (botPlayers.isEmpty()) return;

    int day = game.getDayNumber();

    switch (phase) {
      case NIGHT_WOLVES -> botWolfKill(game, botPlayers, day);
      case NIGHT_SEER   -> botSeerCheck(game, botPlayers, day);
      case NIGHT_WITCH  -> botWitchAct(game, botPlayers, day);
      case DAY_CHAT     -> botChat(game, botPlayers);
      case DAY_VOTE     -> botDayVote(game, botPlayers, day);
      default -> {}
    }
  }

  // -- Wolf bots: pick a random non-wolf alive target --
  private void botWolfKill(Game game, List<GamePlayer> botPlayers, int day) {
    List<GamePlayer> wolfBots = botPlayers.stream()
      .filter(gp -> gp.getRole() == Role.WOLF)
      .toList();

    List<GamePlayer> aliveNonWolves = gpRepo.findByGameOrderByIdAsc(game).stream()
      .filter(gp -> gp.isAlive() && gp.getRole() != Role.WOLF)
      .collect(Collectors.toCollection(ArrayList::new));

    if (aliveNonWolves.isEmpty()) return;

    for (GamePlayer wolf : wolfBots) {
      // Skip if already acted
      boolean alreadyActed = nightRepo
        .findByGameAndDayNumberAndActorAndType(game, day, wolf, NightActionType.WOLF_KILL)
        .isPresent();
      if (alreadyActed) continue;

      // Random delay: only act 50% chance each tick (simulates human think time)
      if (rnd.nextBoolean()) continue;

      // Pick a random target not already chosen
      Set<Long> chosen = nightRepo.findByGameAndDayNumberAndType(game, day, NightActionType.WOLF_KILL)
        .stream().map(a -> a.getTarget().getId()).collect(Collectors.toSet());

      List<GamePlayer> available = aliveNonWolves.stream()
        .filter(p -> !chosen.contains(p.getId()))
        .collect(Collectors.toCollection(ArrayList::new));
      if (available.isEmpty()) available = new ArrayList<>(aliveNonWolves);
      if (available.isEmpty()) continue;

      GamePlayer target = available.get(rnd.nextInt(available.size()));

      try {
        gameService.wolfKill(game.getRoom().getCode(), wolf.getUser(), target.getId());
      } catch (Exception ignored) {}
    }
  }

  // -- Seer bot: investigate a random alive player --
  private void botSeerCheck(Game game, List<GamePlayer> botPlayers, int day) {
    botPlayers.stream()
      .filter(gp -> gp.getRole() == Role.SEER)
      .findFirst()
      .ifPresent(seer -> {
        boolean alreadyActed = nightRepo
          .findByGameAndDayNumberAndActorAndType(game, day, seer, NightActionType.SEER_CHECK)
          .isPresent();
        if (alreadyActed) return;
        if (rnd.nextBoolean()) return; // random delay

        List<GamePlayer> targets = gpRepo.findByGameOrderByIdAsc(game).stream()
          .filter(gp -> gp.isAlive() && !gp.getUser().getId().equals(seer.getUser().getId()))
          .toList();
        if (targets.isEmpty()) return;

        GamePlayer target = targets.get(rnd.nextInt(targets.size()));
        try {
          gameService.seerCheck(game.getRoom().getCode(), seer.getUser(), target.getId());
        } catch (Exception ignored) {}
      });
  }

  // -- Witch bot: always heal victim if can, never poison --
  private void botWitchAct(Game game, List<GamePlayer> botPlayers, int day) {
    botPlayers.stream()
      .filter(gp -> gp.getRole() == Role.WITCH)
      .findFirst()
      .ifPresent(witch -> {
        if (rnd.nextBoolean()) return; // random delay

        // Find wolf victim to maybe heal
        Long healId = null;
        if (witch.isWitchHealAvailable()) {
          List<NightAction> wolfKills = nightRepo.findByGameAndDayNumberAndType(game, day, NightActionType.WOLF_KILL);
          if (!wolfKills.isEmpty()) {
            healId = wolfKills.get(0).getTarget().getId();
          }
        }
        try {
          gameService.witchAct(game.getRoom().getCode(), witch.getUser(), healId, null);
        } catch (Exception ignored) {}
      });
  }

  // -- Bots send a chat message (max 1 per game per 15s) --
  private void botChat(Game game, List<GamePlayer> botPlayers) {
    long now = System.currentTimeMillis();
    long lastChat = lastBotChatTime.getOrDefault(game.getId(), 0L);
    if (now - lastChat < 15_000) return; // one bot message per 15 seconds

    // Pick a random bot to chat
    List<GamePlayer> chatBots = new ArrayList<>(botPlayers);
    if (chatBots.isEmpty()) return;
    GamePlayer chatBot = chatBots.get(rnd.nextInt(chatBots.size()));

    String msg = BOT_CHAT_MESSAGES.get(rnd.nextInt(BOT_CHAT_MESSAGES.size()));
    try {
      gameService.postChat(game.getRoom().getCode(), chatBot.getUser(), msg);
      lastBotChatTime.put(game.getId(), now);
    } catch (Exception ignored) {}
  }

  // -- Bots vote for a random alive non-self player --
  private void botDayVote(Game game, List<GamePlayer> botPlayers, int day) {
    for (GamePlayer bot : botPlayers) {
      boolean alreadyVoted = voteRepo
        .findByGameAndDayNumberAndVoter(game, day, bot)
        .isPresent();
      if (alreadyVoted) continue;
      if (rnd.nextBoolean()) continue; // random delay

      List<GamePlayer> targets = gpRepo.findByGameOrderByIdAsc(game).stream()
        .filter(gp -> gp.isAlive() && !gp.getUser().getId().equals(bot.getUser().getId()))
        .toList();
      if (targets.isEmpty()) continue;

      GamePlayer target = targets.get(rnd.nextInt(targets.size()));
      try {
        gameService.vote(game.getRoom().getCode(), bot.getUser(), target.getId());
      } catch (Exception ignored) {}
    }
  }
}
