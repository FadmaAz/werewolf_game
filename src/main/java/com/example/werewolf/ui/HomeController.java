package com.example.werewolf.ui;

import com.example.werewolf.domain.User;
import com.example.werewolf.repo.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
  private final UserRepository userRepo;

  public HomeController(UserRepository userRepo) {
    this.userRepo = userRepo;
  }

  @GetMapping("/")
  public String index(HttpSession session, Model model) {
    Long userId = (Long) session.getAttribute("USER_ID");
    if (userId != null) {
      userRepo.findById(userId).ifPresent(u -> model.addAttribute("me", u));
    }
    return "index";
  }

  @GetMapping("/play")
  public String play(HttpSession session, Model model) {
    Long userId = (Long) session.getAttribute("USER_ID");
    if (userId == null) return "redirect:/login";
    User me = userRepo.findById(userId).orElse(null);
    model.addAttribute("me", me);

    String roomCode = (String) session.getAttribute("ROOM_CODE");
    model.addAttribute("roomCode", roomCode);

    return "play";
  }
}
