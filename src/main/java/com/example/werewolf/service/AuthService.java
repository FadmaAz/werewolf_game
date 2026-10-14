package com.example.werewolf.service;

import com.example.werewolf.domain.User;
import com.example.werewolf.repo.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
  private final UserRepository userRepo;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  public AuthService(UserRepository userRepo) {
    this.userRepo = userRepo;
  }

  public User register(String username, String rawPassword, String avatar) {
    if (username == null || username.isBlank() || username.trim().length() < 3) {
      throw new IllegalArgumentException("Username doit contenir au moins 3 caractères.");
    }
    if (rawPassword == null || rawPassword.length() < 4) {
      throw new IllegalArgumentException("Mot de passe doit contenir au moins 4 caractères.");
    }
    userRepo.findByUsernameIgnoreCase(username.trim()).ifPresent(u -> {
      throw new IllegalArgumentException("Username déjà utilisé.");
    });

    User u = new User();
    u.setUsername(username.trim());
    u.setPasswordHash(encoder.encode(rawPassword));
    u.setAvatar((avatar == null || avatar.isBlank()) ? "avatar1.png" : avatar.trim());
    return userRepo.save(u);
  }

  public Optional<User> login(String username, String rawPassword) {
    if (username == null || rawPassword == null) return Optional.empty();
    return userRepo.findByUsernameIgnoreCase(username.trim())
      .filter(u -> encoder.matches(rawPassword, u.getPasswordHash()));
  }
}
