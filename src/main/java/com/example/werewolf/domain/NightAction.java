package com.example.werewolf.domain;

import com.example.werewolf.domain.enums.NightActionType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name="night_actions",
  indexes = { @Index(name="idx_night_game_day", columnList = "game_id,dayNumber") })
public class NightAction {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional=false, fetch=FetchType.LAZY)
  private Game game;

  @Column(nullable=false)
  private int dayNumber;

  @ManyToOne(optional=false, fetch=FetchType.LAZY)
  private GamePlayer actor;

  @Enumerated(EnumType.STRING)
  @Column(nullable=false)
  private NightActionType type;

  @ManyToOne(fetch=FetchType.LAZY)
  private GamePlayer target;

  @Column(nullable=false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public Game getGame() { return game; }
  public void setGame(Game game) { this.game = game; }
  public int getDayNumber() { return dayNumber; }
  public void setDayNumber(int dayNumber) { this.dayNumber = dayNumber; }
  public GamePlayer getActor() { return actor; }
  public void setActor(GamePlayer actor) { this.actor = actor; }
  public NightActionType getType() { return type; }
  public void setType(NightActionType type) { this.type = type; }
  public GamePlayer getTarget() { return target; }
  public void setTarget(GamePlayer target) { this.target = target; }
  public Instant getCreatedAt() { return createdAt; }
}
