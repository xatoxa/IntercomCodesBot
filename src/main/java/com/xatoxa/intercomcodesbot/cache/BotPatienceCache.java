package com.xatoxa.intercomcodesbot.cache;

import com.xatoxa.intercomcodesbot.botapi.BotState;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class BotPatienceCache {
    private final Map<Long, Integer> patienceOfUsers = new HashMap<>();

    public Integer get(Long userId){
        Integer patience = patienceOfUsers.get(userId);
        if (patience == null){
            patience = 0;
        }
        patienceOfUsers.put(userId, patience + 1);

        return patience;
    }

    public void reset(Long userId){
        patienceOfUsers.remove(userId);
    }
}
