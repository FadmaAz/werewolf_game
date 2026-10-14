package com.example.werewolf.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name="chat_messages",
  indexes = { @Index(name="idx_chat_game_time", columnList = "game_id,createdAt") })
public class ChatMessage {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional=false, fetch=FetchType.LAZY) //relation est obligatoire
  private Game game;

  @Column(nullable=false)// ne doit pas etre null
  private int dayNumber;

  @ManyToOne(optional=false, fetch=FetchType.LAZY)
  private User sender;

  @Column(nullable=false, length=400)
  private String message;

  @Column(nullable=false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public Game getGame() { return game; }
  public void setGame(Game game) { this.game = game; }
  public int getDayNumber() { return dayNumber; }
  public void setDayNumber(int dayNumber) { this.dayNumber = dayNumber; }
  public User getSender() { return sender; }
  public void setSender(User sender) { this.sender = sender; }
  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }
  public Instant getCreatedAt() { return createdAt; }
}
