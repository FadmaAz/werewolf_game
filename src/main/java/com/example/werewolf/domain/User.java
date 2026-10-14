package com.example.werewolf.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="users", uniqueConstraints = @UniqueConstraint(columnNames = "username"))
public class User {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable=false, length=30)
  private String username;

  @Column(nullable=false, length=200)
  private String passwordHash;

  @Column(nullable=false, length=100)
  private String avatar = "avatar1.png";

  @Column(nullable=false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }
  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
  public String getAvatar() { return avatar; }
  public void setAvatar(String avatar) { this.avatar = avatar; }
  public Instant getCreatedAt() { return createdAt; }
}
