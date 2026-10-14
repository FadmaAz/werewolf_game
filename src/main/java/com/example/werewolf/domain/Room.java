package com.example.werewolf.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="rooms", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class Room {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable=false, length=10)
  private String code;

  @ManyToOne(optional=false, fetch=FetchType.LAZY)
  private User admin;

  @Column(nullable=false)
  private boolean started = false;

  @Column(nullable=false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public User getAdmin() { return admin; }
  public void setAdmin(User admin) { this.admin = admin; }
  public boolean isStarted() { return started; }
  public void setStarted(boolean started) { this.started = started; }
  public Instant getCreatedAt() { return createdAt; }
}
