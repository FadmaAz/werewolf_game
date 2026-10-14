package com.example.werewolf.service;

import com.example.werewolf.domain.User;
import com.example.werewolf.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final UserRepository userRepo;

  public AuthService(UserRepository userRepo) {
    this.userRepo = userRepo;
  }

  /**
   * Login or create a user by pseudo only (no password).
   * If the username already exists → returns existing user.
   * If not → creates a new user with the given avatar.
   */
  @Transactional
  public User loginOrCreate(String username, String avatar) {
    if (username == null || username.isBlank() || username.trim().length() < 3) {
      throw new IllegalArgumentException("Le pseudo doit contenir au moins 3 caractères.");
    }
    if (username.trim().length() > 20) {
      throw new IllegalArgumentException("Le pseudo ne peut pas dépasser 20 caractères.");
    }
    String cleanName = username.trim();
    String cleanAvatar = (avatar == null || avatar.isBlank()) ? "avatar1.png" : avatar.trim();

    return userRepo.findByUsernameIgnoreCase(cleanName).orElseGet(() -> {
      User u = new User();
      u.setUsername(cleanName);
      u.setAvatar(cleanAvatar);
      u.setBot(false);
      return userRepo.save(u);
    });
  }
}
