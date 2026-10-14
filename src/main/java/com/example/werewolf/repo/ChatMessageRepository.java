package com.example.werewolf.repo;

import com.example.werewolf.domain.ChatMessage;
import com.example.werewolf.domain.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
  List<ChatMessage> findTop100ByGameOrderByCreatedAtAsc(Game game);
  List<ChatMessage> findByGameAndIdGreaterThanOrderByIdAsc(Game game, long afterId);
}
