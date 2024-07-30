package com.xatoxa.intercomcodesbot.repository;

import com.xatoxa.intercomcodesbot.entity.UserHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserHistoryRepository extends JpaRepository<UserHistory, Long> {
    List<UserHistory> findAllByUserIdAndActionTypeNotOrderByDateTime(Long userId, String actionType);

    List<UserHistory> findAllByActionTypeNot(String actionType);
    List<UserHistory> findAllByActionType(String actionType);
}
