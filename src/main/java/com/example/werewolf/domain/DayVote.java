package com.example.werewolf.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
// Un joueur (voter_id)
// Un joueur = un seul vote par jour dans une partie
//récupérer rapidement tous les votes d'un jour donné
@Table(name="day_votes",
  uniqueConstraints = @UniqueConstraint(columnNames = {"game_id","dayNumber","voter_id"}),
  indexes = { @Index(name="idx_vote_game_day", columnList = "game_id,dayNumber") })
public class DayVote {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional=false, fetch=FetchType.LAZY)
  private Game game;

  @Column(nullable=false)
  private int dayNumber;

  @ManyToOne(optional=false, fetch=FetchType.LAZY)
  private GamePlayer voter;

  @ManyToOne(optional=false, fetch=FetchType.LAZY)
  private GamePlayer target;

  @Column(nullable=false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public Game getGame() { return game; }
  public void setGame(Game game) { this.game = game; }
  public int getDayNumber() { return dayNumber; }
  public void setDayNumber(int dayNumber) { this.dayNumber = dayNumber; }
  public GamePlayer getVoter() { return voter; }
  public void setVoter(GamePlayer voter) { this.voter = voter; }
  public GamePlayer getTarget() { return target; }
  public void setTarget(GamePlayer target) { this.target = target; }
  public Instant getCreatedAt() { return createdAt; }
}
