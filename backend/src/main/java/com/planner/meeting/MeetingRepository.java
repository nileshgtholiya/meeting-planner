package com.planner.meeting;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    List<Meeting> findByOwnerIdOrderByStartTimeAsc(Long ownerId);
}
