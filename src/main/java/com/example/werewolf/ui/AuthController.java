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

  /** POST /join → pseudo-only login or account creation */
  @PostMapping("/join")
  public String join(@RequestParam("username") String username,
                     @RequestParam(value="avatar", required=false) String avatar,
                     HttpSession session,
                     Model model) {
    try {
      User u = authService.loginOrCreate(username, avatar);
      session.setAttribute("USER_ID", u.getId());
      return "redirect:/play";
    } catch (IllegalArgumentException ex) {
      model.addAttribute("error", ex.getMessage());
      return "index";
    }
  }

  @PostMapping("/logout")
  public String logout(HttpSession session) {
    session.invalidate();
    return "redirect:/";
  }
}
