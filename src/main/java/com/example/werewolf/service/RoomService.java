package com.example.werewolf.service;

import com.example.werewolf.domain.Room;
import com.example.werewolf.domain.RoomMember;
import com.example.werewolf.domain.User;
import com.example.werewolf.repo.RoomMemberRepository;
import com.example.werewolf.repo.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
public class RoomService {
  private final RoomRepository roomRepo;
  private final RoomMemberRepository memberRepo;
  private final SecureRandom rnd = new SecureRandom();

  public RoomService(RoomRepository roomRepo, RoomMemberRepository memberRepo) {
    this.roomRepo = roomRepo;
    this.memberRepo = memberRepo;
  }

  @Transactional
  public Room createRoom(User admin) {
    Room r = new Room();
    r.setAdmin(admin);
    r.setCode(generateCode());
    r.setStarted(false);
    r = roomRepo.save(r);

    RoomMember rm = new RoomMember();
    rm.setRoom(r);
    rm.setUser(admin);
    memberRepo.save(rm);

    return r;
  }

  @Transactional
  public Room joinRoom(String code, User user) {
    Room r = roomRepo.findByCode(code).orElseThrow(() -> new IllegalArgumentException("Room introuvable."));
    if (r.isStarted()) {
      throw new IllegalArgumentException("La partie a déjà démarré, impossible de rejoindre.");
    }
    if (memberRepo.findByRoomAndUser(r, user).isEmpty()) {
      RoomMember rm = new RoomMember();
      rm.setRoom(r);
      rm.setUser(user);
      memberRepo.save(rm);
    }
    return r;
  }

  private String generateCode() {
    String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    while (true) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < 6; i++) sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
      String code = sb.toString();
      if (!roomRepo.existsByCode(code)) return code;
    }
  }
}
