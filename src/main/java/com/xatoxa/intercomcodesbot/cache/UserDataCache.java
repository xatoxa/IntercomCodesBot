package com.xatoxa.intercomcodesbot.cache;

import com.xatoxa.intercomcodesbot.botapi.BotState;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class UserDataCache implements DataCache{
    private final Map<Long, BotState> usersBotState = new HashMap<>();
    private final Map<Long, CodeCache> usersCodeCache = new HashMap<>();

    @Override
    public void setUsersCurrentBotState(Long userId, BotState botState) {
        usersBotState.put(userId, botState);
    }

    @Override
    public BotState getUsersCurrentBotState(Long userId) {
        BotState botState = usersBotState.get(userId);
        if (botState == null){
            botState = BotState.DEFAULT;
        }

        return botState;
    }

    @Override
    public void setUsersCurrentCodeCache(Long userId, CodeCache codeCache) {
        usersCodeCache.put(userId, codeCache);
    }

    @Override
    public CodeCache getUsersCurrentCodeCache(Long userId) {
        CodeCache codeCache = usersCodeCache.get(userId);
        if (codeCache == null){
            codeCache = new CodeCache();
        }

        return codeCache;
    }

    public void removeUsersCurrentCodeCache(Long userId){
        usersCodeCache.remove(userId);
    }

}
