package com.example.werewolf.repo;

import com.example.werewolf.domain.Game;
import com.example.werewolf.domain.GamePlayer;
import com.example.werewolf.domain.NightAction;
import com.example.werewolf.domain.enums.NightActionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NightActionRepository extends JpaRepository<NightAction, Long> {
  List<NightAction> findByGameAndDayNumber(Game game, int dayNumber);
  List<NightAction> findByGameAndDayNumberAndType(Game game, int dayNumber, NightActionType type);
  Optional<NightAction> findByGameAndDayNumberAndActorAndType(Game game, int dayNumber, GamePlayer actor, NightActionType type);
}
