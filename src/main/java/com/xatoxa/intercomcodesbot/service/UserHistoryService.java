package com.xatoxa.intercomcodesbot.service;

import com.xatoxa.intercomcodesbot.entity.UserHistory;
import com.xatoxa.intercomcodesbot.repository.UserHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserHistoryService {
    @Autowired
    UserHistoryRepository userHistoryRepository;

    public void save(UserHistory userHistory){
        userHistoryRepository.save(userHistory);
    }

    public List<UserHistory> findAll(){
        return userHistoryRepository.findAll();
    }

    public List<UserHistory> findAllByUserId(long userId) {
        return userHistoryRepository.findAllByUserIdOrderByDateTime(userId);
    }
}
