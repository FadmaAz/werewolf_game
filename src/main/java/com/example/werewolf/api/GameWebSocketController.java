package com.example.werewolf.api;

import com.example.werewolf.service.GameService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * Minimal STOMP controller. Game actions still go through the classic HTTP
 * forms (submit + redirect); this controller only lets a freshly connected
 * client ask the server to broadcast the current state.
 */
@Controller
public class GameWebSocketController {

  private final GameService gameService;

  public GameWebSocketController(GameService gameService) {
    this.gameService = gameService;
  }

  @MessageMapping("/game/{code}/sync")
  public void sync(@DestinationVariable("code") String code) {
    gameService.syncState(code);
  }
}
