package com.example.werewolf.repo;

import com.example.werewolf.domain.DayVote;
import com.example.werewolf.domain.Game;
import com.example.werewolf.domain.GamePlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DayVoteRepository extends JpaRepository<DayVote, Long> {
  List<DayVote> findByGameAndDayNumber(Game game, int dayNumber);
  Optional<DayVote> findByGameAndDayNumberAndVoter(Game game, int dayNumber, GamePlayer voter);
  void deleteByGameAndDayNumber(Game game, int dayNumber);
}
