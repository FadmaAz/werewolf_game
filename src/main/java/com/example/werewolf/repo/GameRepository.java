package com.example.werewolf.repo;

import com.example.werewolf.domain.Game;
import com.example.werewolf.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {
  Optional<Game> findByRoom(Room room);
  Optional<Game> findByRoom_Code(String roomCode);
}
