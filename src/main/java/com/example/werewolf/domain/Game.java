package com.example.werewolf.domain;

import com.example.werewolf.domain.enums.GameStatus;
import com.example.werewolf.domain.enums.Phase;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name="games", uniqueConstraints = @UniqueConstraint(columnNames = {"room_id"}))
public class Game {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(optional=false, fetch=FetchType.LAZY)
  private Room room;

  @Enumerated(EnumType.STRING)
  @Column(nullable=false)
  private GameStatus status = GameStatus.LOBBY;

  @Enumerated(EnumType.STRING)
  @Column(nullable=false)
  private Phase phase = Phase.LOBBY;

  @Column(nullable=false)
  private int dayNumber = 0;

  private Instant phaseEndsAt;

  @Column(nullable=false)
  private Instant createdAt = Instant.now();

  @Column(length=20)
  private String winner; // "VILLAGE" or "WOLVES" or null

  public Long getId() { return id; }
  public Room getRoom() { return room; }
  public void setRoom(Room room) { this.room = room; }
  public GameStatus getStatus() { return status; }
  public void setStatus(GameStatus status) { this.status = status; }
  public Phase getPhase() { return phase; }
  public void setPhase(Phase phase) { this.phase = phase; }
  public int getDayNumber() { return dayNumber; }
  public void setDayNumber(int dayNumber) { this.dayNumber = dayNumber; }
  public Instant getPhaseEndsAt() { return phaseEndsAt; }
  public void setPhaseEndsAt(Instant phaseEndsAt) { this.phaseEndsAt = phaseEndsAt; }
  public Instant getCreatedAt() { return createdAt; }
  public String getWinner() { return winner; }
  public void setWinner(String winner) { this.winner = winner; }
}
