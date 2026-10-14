package com.example.werewolf.domain;

import com.example.werewolf.domain.enums.Role;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name="game_players", uniqueConstraints = @UniqueConstraint(columnNames = {"game_id","user_id"}))
public class GamePlayer {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional=false, fetch=FetchType.LAZY)
  private Game game;

  @ManyToOne(optional=false, fetch=FetchType.LAZY)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable=false)
  private Role role = Role.VILLAGER;

  @Column(nullable=false)
  private boolean alive = true;

  // Witch potions
  @Column(nullable=false)
  private boolean witchHealAvailable = false;

  @Column(nullable=false)
  private boolean witchPoisonAvailable = false;

  private Instant eliminatedAt;

  public Long getId() { return id; }
  public Game getGame() { return game; }
  public void setGame(Game game) { this.game = game; }
  public User getUser() { return user; }
  public void setUser(User user) { this.user = user; }
  public Role getRole() { return role; }
  public void setRole(Role role) { this.role = role; }
  public boolean isAlive() { return alive; }
  public void setAlive(boolean alive) { this.alive = alive; }
  public boolean isWitchHealAvailable() { return witchHealAvailable; }
  public void setWitchHealAvailable(boolean witchHealAvailable) { this.witchHealAvailable = witchHealAvailable; }
  public boolean isWitchPoisonAvailable() { return witchPoisonAvailable; }
  public void setWitchPoisonAvailable(boolean witchPoisonAvailable) { this.witchPoisonAvailable = witchPoisonAvailable; }
  public Instant getEliminatedAt() { return eliminatedAt; }
  public void setEliminatedAt(Instant eliminatedAt) { this.eliminatedAt = eliminatedAt; }
}
