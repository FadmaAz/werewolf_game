package com.example.werewolf.api.dto;

public class GameStateDto {
  public String roomCode;
  public String status;
  public String phase;
  public int dayNumber;
  public long serverNowMs;
  public long phaseEndsAtMs;

  public boolean meAlive;
  public String meRole;
  public boolean isAdmin;

  public GameStateDto() {}
}
