package com.example.werewolf.domain;

import com.example.werewolf.domain.enums.EventVisibility;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name="game_events",
  indexes = { @Index(name="idx_event_game_time", columnList = "game_id,createdAt") })
public class GameEvent {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional=false, fetch=FetchType.LAZY)
  private Game game;

  @Column(nullable=false)
  private int dayNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable=false)
  private EventVisibility visibility = EventVisibility.PUBLIC;

  @ManyToOne(fetch=FetchType.LAZY)
  private User recipient;

  @Column(nullable=false, length=400)
  private String message;

  @Column(nullable=false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public Game getGame() { return game; }
  public void setGame(Game game) { this.game = game; }
  public int getDayNumber() { return dayNumber; }
  public void setDayNumber(int dayNumber) { this.dayNumber = dayNumber; }
  public EventVisibility getVisibility() { return visibility; }
  public void setVisibility(EventVisibility visibility) { this.visibility = visibility; }
  public User getRecipient() { return recipient; }
  public void setRecipient(User recipient) { this.recipient = recipient; }
  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }
  public Instant getCreatedAt() { return createdAt; }
}
