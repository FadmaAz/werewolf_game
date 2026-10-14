package com.example.werewolf.repo;

import com.example.werewolf.domain.Room;
import com.example.werewolf.domain.RoomMember;
import com.example.werewolf.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {
  List<RoomMember> findByRoomOrderByJoinedAtAsc(Room room);
  Optional<RoomMember> findByRoomAndUser(Room room, User user);
  long countByRoom(Room room);
}
