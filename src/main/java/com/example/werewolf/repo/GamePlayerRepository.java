package com.example.werewolf.repo;

import com.example.werewolf.domain.Game;
import com.example.werewolf.domain.GamePlayer;
import com.example.werewolf.domain.User;
import com.example.werewolf.domain.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GamePlayerRepository extends JpaRepository<GamePlayer, Long> {
  List<GamePlayer> findByGameOrderByIdAsc(Game game);
  Optional<GamePlayer> findByGameAndUser(Game game, User user);
  long countByGameAndAliveTrue(Game game);
  long countByGameAndAliveTrueAndRole(Game game, Role role);
  List<GamePlayer> findByGameAndAliveTrueAndRole(Game game, Role role);
}
