package com.example.werewolf.service;

import com.example.werewolf.api.dto.ChatMessageDto;
import com.example.werewolf.api.dto.GameEventDto;
import com.example.werewolf.api.dto.GameStateDto;
import com.example.werewolf.api.dto.RoomUpdateDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Broadcasts real-time updates to all connected clients via WebSocket (STOMP).
 */
@Service
public class WebSocketPublisher {

  private final SimpMessagingTemplate ws;

  public WebSocketPublisher(SimpMessagingTemplate ws) {
    this.ws = ws;
  }

  /** Push game state (phase, timer, alive count) to all players in the game. */
  public void publishState(String roomCode, GameStateDto state) {
    ws.convertAndSend("/topic/game/" + roomCode + "/state", state);
  }

  /** Push a public game event (elimination, phase change…) to all players. */
  public void publishEvent(String roomCode, GameEventDto event) {
    ws.convertAndSend("/topic/game/" + roomCode + "/events", event);
  }

  /** Push a private event to a specific player (wolf vision, seer result…). */
  public void publishPrivateEvent(String userId, GameEventDto event) {
    ws.convertAndSend("/topic/private/" + userId, event);
  }

  /** Push a new chat message to all players in the game. */
  public void publishChat(String roomCode, ChatMessageDto msg) {
    ws.convertAndSend("/topic/game/" + roomCode + "/chat", msg);
  }

  /** Push a room update — roomCode extracted from the DTO. */
  public void publishRoomUpdate(RoomUpdateDto update) {
    ws.convertAndSend("/topic/room/" + update.roomCode, update);
  }

  /** Push a room update with an explicit roomCode. */
  public void publishRoomUpdate(String roomCode, RoomUpdateDto update) {
    ws.convertAndSend("/topic/room/" + roomCode, update);
  }
}
