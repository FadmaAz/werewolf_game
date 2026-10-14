package com.example.werewolf.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="room_members", uniqueConstraints = @UniqueConstraint(columnNames = {"room_id","user_id"}))
public class RoomMember {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional=false, fetch=FetchType.LAZY)
  private Room room;

  @ManyToOne(optional=false, fetch=FetchType.LAZY)
  private User user;

  @Column(nullable=false)
  private Instant joinedAt = Instant.now();

  public Long getId() { return id; }
  public Room getRoom() { return room; }
  public void setRoom(Room room) { this.room = room; }
  public User getUser() { return user; }
  public void setUser(User user) { this.user = user; }
  public Instant getJoinedAt() { return joinedAt; }
}
