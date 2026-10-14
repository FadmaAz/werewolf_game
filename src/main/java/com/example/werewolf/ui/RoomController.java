package com.example.werewolf.ui;

import com.example.werewolf.domain.Game;
import com.example.werewolf.domain.Room;
import com.example.werewolf.domain.User;
import com.example.werewolf.repo.RoomMemberRepository;
import com.example.werewolf.repo.RoomRepository;
import com.example.werewolf.repo.UserRepository;
import com.example.werewolf.service.GameService;
import com.example.werewolf.service.RoomService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/room")
public class RoomController {

  private final UserRepository userRepo;
  private final RoomRepository roomRepo;
  private final RoomMemberRepository memberRepo;
  private final RoomService roomService;
  private final GameService gameService;

  public RoomController(UserRepository userRepo,
                        RoomRepository roomRepo,
                        RoomMemberRepository memberRepo,
                        RoomService roomService,
                        GameService gameService) {
    this.userRepo = userRepo;
    this.roomRepo = roomRepo;
    this.memberRepo = memberRepo;
    this.roomService = roomService;
    this.gameService = gameService;
  }

  @PostMapping("/create")
  public String create(HttpSession session) {
    Long userId = (Long) session.getAttribute("USER_ID");
    if (userId == null) return "redirect:/login";
    User me = userRepo.findById(userId).orElseThrow();
    Room r = roomService.createRoom(me);
    session.setAttribute("ROOM_CODE", r.getCode());
    return "redirect:/room/" + r.getCode();
  }

  @PostMapping("/join")
  public String join(@RequestParam("code") String code, HttpSession session, RedirectAttributes ra) {
    Long userId = (Long) session.getAttribute("USER_ID");
    if (userId == null) return "redirect:/login";
    User me = userRepo.findById(userId).orElseThrow();

    try {
      Room r = roomService.joinRoom(code.trim().toUpperCase(), me);
      session.setAttribute("ROOM_CODE", r.getCode());
      return "redirect:/room/" + r.getCode();
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
      return "redirect:/play";
    }
  }

  @GetMapping("/{code}")
  public String room(@PathVariable("code") String code, HttpSession session, Model model) {
    Long userId = (Long) session.getAttribute("USER_ID");
    if (userId == null) return "redirect:/login";
    User me = userRepo.findById(userId).orElseThrow();
    Room room = roomRepo.findByCode(code).orElseThrow();

    model.addAttribute("me", me);
    model.addAttribute("room", room);
    model.addAttribute("isAdmin", room.getAdmin().getId().equals(me.getId()));
    model.addAttribute("members", memberRepo.findByRoomOrderByJoinedAtAsc(room));
    model.addAttribute("count", memberRepo.countByRoom(room));
    model.addAttribute("gameStarted", room.isStarted());
    model.addAttribute("game", gameService.findGameByRoomCode(room.getCode()).orElse(null));

    return "room";
  }

  @PostMapping("/{code}/start")
  public String start(@PathVariable("code") String code, HttpSession session, RedirectAttributes ra) {
    Long userId = (Long) session.getAttribute("USER_ID");
    if (userId == null) return "redirect:/login";
    User me = userRepo.findById(userId).orElseThrow();
    try {
      gameService.startGame(code, me);
      return "redirect:/game/" + code;
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
      return "redirect:/room/" + code;
    }
  }
}
