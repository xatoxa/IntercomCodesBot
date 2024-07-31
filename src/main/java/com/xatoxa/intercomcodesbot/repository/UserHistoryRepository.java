package com.xatoxa.intercomcodesbot.repository;

import com.xatoxa.intercomcodesbot.entity.Home;
import com.xatoxa.intercomcodesbot.entity.UserHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserHistoryRepository extends JpaRepository<UserHistory, Long> {
    List<UserHistory> findAllByUserIdAndActionTypeNotOrderByDateTime(Long userId, String actionType);

    List<UserHistory> findAllByActionTypeNot(String actionType);
    List<UserHistory> findAllByActionTypeOrderByDateTimeAsc(String actionType);

    List<UserHistory> findAllByActionTypeAndDateTimeAfter(String actionType, LocalDateTime currentDate);
}
