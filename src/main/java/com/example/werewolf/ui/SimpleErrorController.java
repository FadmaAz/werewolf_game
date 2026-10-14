package com.example.werewolf.ui;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SimpleErrorController implements ErrorController {

  @RequestMapping("/error")
  public String handleError(HttpServletRequest request, Model model) {
    Object status = request.getAttribute("jakarta.servlet.error.status_code");
    Object message = request.getAttribute("jakarta.servlet.error.message");

    String msg = "Erreur";
    if (status != null) msg = "Erreur " + status;
    if (message != null && !message.toString().isBlank()) msg += " - " + message;

    model.addAttribute("message", msg);
    return "error";
  }
}
