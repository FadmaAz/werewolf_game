package com.example.werewolf.repo;

import com.example.werewolf.domain.Game;
import com.example.werewolf.domain.GameEvent;
import com.example.werewolf.domain.User;
import com.example.werewolf.domain.enums.EventVisibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameEventRepository extends JpaRepository<GameEvent, Long> {
  List<GameEvent> findTop200ByGameOrderByCreatedAtAsc(Game game);
  List<GameEvent> findByGameAndIdGreaterThanOrderByIdAsc(Game game, long afterId);
  List<GameEvent> findByGameAndVisibilityAndIdGreaterThanOrderByIdAsc(Game game, EventVisibility visibility, long afterId);
  List<GameEvent> findByGameAndRecipientAndVisibilityAndIdGreaterThanOrderByIdAsc(Game game, User recipient, EventVisibility visibility, long afterId);
}
