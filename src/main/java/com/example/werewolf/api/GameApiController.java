package com.example.werewolf.api;

import com.example.werewolf.api.dto.ChatMessageDto;
import com.example.werewolf.api.dto.GameEventDto;
import com.example.werewolf.api.dto.GameStateDto;
import com.example.werewolf.domain.Game;
import com.example.werewolf.domain.GameEvent;
import com.example.werewolf.domain.GamePlayer;
import com.example.werewolf.domain.User;
import com.example.werewolf.domain.enums.EventVisibility;
import com.example.werewolf.repo.GameRepository;
import com.example.werewolf.repo.RoomRepository;
import com.example.werewolf.repo.UserRepository;
import com.example.werewolf.service.GameService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/game")
public class GameApiController {

  private final UserRepository userRepo;
  private final RoomRepository roomRepo;
  private final GameRepository gameRepo;
  private final GameService gameService;

  public GameApiController(UserRepository userRepo, RoomRepository roomRepo, GameRepository gameRepo, GameService gameService) {
    this.userRepo = userRepo;
    this.roomRepo = roomRepo;
    this.gameRepo = gameRepo;
    this.gameService = gameService;
  }

  private User requireUser(HttpSession session) {
    Long userId = (Long) session.getAttribute("USER_ID");
    if (userId == null) throw new IllegalArgumentException("Not logged");
    return userRepo.findById(userId).orElseThrow();
  }

  @GetMapping("/{code}/state")
  public ResponseEntity<GameStateDto> state(@PathVariable("code") String code, HttpSession session) {
    try {
      User me = requireUser(session);
      if (roomRepo.findByCode(code).isEmpty()) return ResponseEntity.notFound().build();
      Game game = gameRepo.findByRoom_Code(code).orElse(null);
      if (game == null) return ResponseEntity.notFound().build();

      gameService.tickIfNeeded(game);

      GamePlayer myGp = gameService.requireGamePlayer(game, me);

      GameStateDto dto = new GameStateDto();
      dto.roomCode = code;
      dto.status = game.getStatus().name();
      dto.phase = game.getPhase().name();
      dto.dayNumber = game.getDayNumber();
      dto.serverNowMs = Instant.now().toEpochMilli();
      dto.phaseEndsAtMs = game.getPhaseEndsAt() == null ? 0L : game.getPhaseEndsAt().toEpochMilli();
      dto.meAlive = myGp.isAlive();
      dto.meRole = myGp.getRole().name();
      dto.isAdmin = game.getRoom().getAdmin().getId().equals(me.getId());

      return ResponseEntity.ok(dto);
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.status(401).build();
    }
  }

  @GetMapping("/{code}/chat")
  public ResponseEntity<List<ChatMessageDto>> chat(@PathVariable("code") String code,
                                                   @RequestParam(value="afterId", required=false, defaultValue="0") long afterId,
                                                   HttpSession session) {
    try {
      User me = requireUser(session);
      Game game = gameRepo.findByRoom_Code(code).orElse(null);
      if (game == null) return ResponseEntity.notFound().build();

      // ensure membership
      gameService.requireGamePlayer(game, me);

      var msgs = gameService.getChat(game, afterId).stream().map(m -> {
        ChatMessageDto d = new ChatMessageDto();
        d.id = m.getId();
        d.username = m.getSender().getUsername();
        d.avatar = m.getSender().getAvatar();
        d.message = m.getMessage();
        d.createdAtMs = m.getCreatedAt().toEpochMilli();
        return d;
      }).toList();

      return ResponseEntity.ok(msgs);
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.status(401).build();
    }
  }

  @GetMapping("/{code}/events")
  public ResponseEntity<List<GameEventDto>> events(@PathVariable("code") String code,
                                                   @RequestParam(value="afterId", required=false, defaultValue="0") long afterId,
                                                   HttpSession session) {
    try {
      User me = requireUser(session);
      Game game = gameRepo.findByRoom_Code(code).orElse(null);
      if (game == null) return ResponseEntity.notFound().build();

      // ensure membership
      gameService.requireGamePlayer(game, me);

      List<GameEvent> events = gameService.getVisibleEvents(game, me, afterId);

      List<GameEventDto> dtos = events.stream().map(e -> {
        GameEventDto d = new GameEventDto();
        d.id = e.getId();
        d.message = e.getMessage();
        d.isPrivate = e.getVisibility() == EventVisibility.PRIVATE;
        d.createdAtMs = e.getCreatedAt().toEpochMilli();
        return d;
      }).toList();

      return ResponseEntity.ok(dtos);
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.status(401).build();
    }
  }
}
