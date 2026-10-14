package com.example.werewolf.ui;

import com.example.werewolf.domain.*;
import com.example.werewolf.domain.enums.Phase;
import com.example.werewolf.domain.enums.Role;
import com.example.werewolf.repo.GamePlayerRepository;
import com.example.werewolf.repo.GameRepository;
import com.example.werewolf.repo.RoomRepository;
import com.example.werewolf.repo.UserRepository;
import com.example.werewolf.service.GameService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/game")
public class GameController {

  private final UserRepository userRepo;
  private final RoomRepository roomRepo;
  private final GameRepository gameRepo;
  private final GamePlayerRepository gpRepo;
  private final GameService gameService;

  public GameController(UserRepository userRepo,
                        RoomRepository roomRepo,
                        GameRepository gameRepo,
                        GamePlayerRepository gpRepo,
                        GameService gameService) {
    this.userRepo = userRepo;
    this.roomRepo = roomRepo;
    this.gameRepo = gameRepo;
    this.gpRepo = gpRepo;
    this.gameService = gameService;
  }

  @GetMapping("/{code}")
  public String game(@PathVariable("code") String code, HttpSession session, Model model) {
    Long userId = (Long) session.getAttribute("USER_ID");
    if (userId == null) return "redirect:/login";
    User me = userRepo.findById(userId).orElseThrow();

    Room room = roomRepo.findByCode(code).orElseThrow();
    if (!room.isStarted()) return "redirect:/room/" + code;

    Game game = gameRepo.findByRoom_Code(code).orElseThrow();
    gameService.tickIfNeeded(game);

    GamePlayer myGp = gameService.requireGamePlayer(game, me);

    List<GamePlayer> players = gpRepo.findByGameOrderByIdAsc(game);
    model.addAttribute("me", me);
    model.addAttribute("room", room);
    model.addAttribute("game", game);
    model.addAttribute("myGp", myGp);
    model.addAttribute("players", players);
    model.addAttribute("isAdmin", room.getAdmin().getId().equals(me.getId()));

    long endsAtMs = game.getPhaseEndsAt() == null ? 0L : game.getPhaseEndsAt().toEpochMilli();
    model.addAttribute("phaseEndsAtMs", endsAtMs);
    model.addAttribute("serverNowMs", Instant.now().toEpochMilli());

    // For wolves UI: chosen targets
    if (game.getPhase() == Phase.NIGHT_WOLVES && myGp.isAlive() && myGp.getRole() == Role.WOLF) {
      Set<Long> chosen = gameService.getWolfChosenTargets(game);
      Long myTarget = gameService.getMyWolfTarget(game, myGp).orElse(null);
      // targets already chosen by others:
      Set<Long> takenByOthers = chosen;
      if (myTarget != null) {
        takenByOthers = chosen.stream().filter(id -> !id.equals(myTarget)).collect(Collectors.toSet());
      }
      model.addAttribute("wolfTakenTargets", takenByOthers);
      model.addAttribute("myWolfTarget", myTarget);
    }

    // For witch: show wolf victims
    if (game.getPhase() == Phase.NIGHT_WITCH && myGp.isAlive() && myGp.getRole() == Role.WITCH) {
      Set<Long> wolfVictimIds = gameService.getWolfChosenTargets(game);
      List<GamePlayer> victims = players.stream().filter(p -> wolfVictimIds.contains(p.getId()) && p.isAlive()).toList();
      model.addAttribute("wolfVictims", victims);
    }

    return "game";
  }

  @PostMapping("/{code}/wolf-kill")
  public String wolfKill(@PathVariable("code") String code,
                         @RequestParam("targetId") long targetId,
                         HttpSession session,
                         RedirectAttributes ra) {
    Long userId = (Long) session.getAttribute("USER_ID");
    if (userId == null) return "redirect:/login";
    User me = userRepo.findById(userId).orElseThrow();
    try {
      gameService.wolfKill(code, me, targetId);
      return "redirect:/game/" + code;
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
      return "redirect:/game/" + code;
    }
  }

  @PostMapping("/{code}/seer-check")
  public String seerCheck(@PathVariable("code") String code,
                          @RequestParam("targetId") long targetId,
                          HttpSession session,
                          RedirectAttributes ra) {
    Long userId = (Long) session.getAttribute("USER_ID");
    if (userId == null) return "redirect:/login";
    User me = userRepo.findById(userId).orElseThrow();
    try {
      gameService.seerCheck(code, me, targetId);
      return "redirect:/game/" + code;
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
      return "redirect:/game/" + code;
    }
  }

  @PostMapping("/{code}/witch-act")
  public String witchAct(@PathVariable("code") String code,
                         @RequestParam(value="healTargetId", required=false) Long healTargetId,
                         @RequestParam(value="poisonTargetId", required=false) Long poisonTargetId,
                         HttpSession session,
                         RedirectAttributes ra) {
    Long userId = (Long) session.getAttribute("USER_ID");
    if (userId == null) return "redirect:/login";
    User me = userRepo.findById(userId).orElseThrow();
    try {
      // empty strings may come -> handle null is fine
      gameService.witchAct(code, me, healTargetId, poisonTargetId);
      return "redirect:/game/" + code;
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
      return "redirect:/game/" + code;
    }
  }

  @PostMapping("/{code}/chat")
  public String chat(@PathVariable("code") String code,
                     @RequestParam("message") String message,
                     HttpSession session,
                     RedirectAttributes ra) {
    Long userId = (Long) session.getAttribute("USER_ID");
    if (userId == null) return "redirect:/login";
    User me = userRepo.findById(userId).orElseThrow();
    try {
      gameService.postChat(code, me, message);
      return "redirect:/game/" + code;
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
      return "redirect:/game/" + code;
    }
  }

  @PostMapping("/{code}/vote")
  public String vote(@PathVariable("code") String code,
                     @RequestParam("targetId") long targetId,
                     HttpSession session,
                     RedirectAttributes ra) {
    Long userId = (Long) session.getAttribute("USER_ID");
    if (userId == null) return "redirect:/login";
    User me = userRepo.findById(userId).orElseThrow();
    try {
      gameService.vote(code, me, targetId);
      return "redirect:/game/" + code;
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
      return "redirect:/game/" + code;
    }
  }
}
