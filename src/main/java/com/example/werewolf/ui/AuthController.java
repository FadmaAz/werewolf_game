package com.example.werewolf.ui;

import com.example.werewolf.domain.User;
import com.example.werewolf.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @GetMapping("/login")
  public String loginForm() {
    return "login";
  }

  @PostMapping("/login")
  public String login(@RequestParam("username") String username,
                      @RequestParam("password") String password,
                      HttpSession session,
                      Model model) {
    return authService.login(username, password)
      .map(u -> {
        session.setAttribute("USER_ID", u.getId());
        return "redirect:/play";
      })
      .orElseGet(() -> {
        model.addAttribute("error", "Identifiants invalides.");
        return "login";
      });
  }

  @GetMapping("/signup")
  public String signupForm() {
    return "signup";
  }

  @PostMapping("/signup")
  public String signup(@RequestParam("username") String username,
                       @RequestParam("password") String password,
                       @RequestParam(value="avatar", required=false) String avatar,
                       HttpSession session,
                       Model model) {
    try {
      User u = authService.register(username, password, avatar);
      session.setAttribute("USER_ID", u.getId());
      return "redirect:/play";
    } catch (IllegalArgumentException ex) {
      model.addAttribute("error", ex.getMessage());
      return "signup";
    }
  }

  @PostMapping("/logout")
  public String logout(HttpSession session) {
    session.invalidate();
    return "redirect:/";
  }
}
