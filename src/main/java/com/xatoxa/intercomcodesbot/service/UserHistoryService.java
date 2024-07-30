package com.xatoxa.intercomcodesbot.service;

import com.xatoxa.intercomcodesbot.ActionType;
import com.xatoxa.intercomcodesbot.entity.UserHistory;
import com.xatoxa.intercomcodesbot.repository.UserHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserHistoryService {
    String SEARCH = ActionType.SEARCH.name();

    @Autowired
    UserHistoryRepository userHistoryRepository;

    public void save(UserHistory userHistory){
        userHistoryRepository.save(userHistory);
    }

    public List<UserHistory> findAll(){
        return userHistoryRepository.findAll();
    }

    public List<UserHistory> findAllByUserId(long userId) {
        return userHistoryRepository.findAllByUserIdAndActionTypeNotOrderByDateTime(userId, SEARCH);
    }

    public List<UserHistory> findAllByActionTypeNotSearch(){
        return userHistoryRepository.findAllByActionTypeNot(SEARCH);
    }

    public List<UserHistory> findAllByActionTypeIsSearch(){
        return userHistoryRepository.findAllByActionType(SEARCH);
    }
}
