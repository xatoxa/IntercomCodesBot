package com.xatoxa.intercomcodesbot.cache;

import com.xatoxa.intercomcodesbot.botapi.BotState;
import org.aopalliance.reflect.Code;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class UserDataCache implements DataCache{
    private Map<Integer, BotState> usersBotState = new HashMap<>();
    private Map<Integer, CodeCache> usersCodeCache = new HashMap<>();

    @Override
    public void setUsersCurrentBotState(int userId, BotState botState) {
        usersBotState.put(userId, botState);
    }

    @Override
    public BotState getUsersCurrentBotState(int userId) {
        BotState botState = usersBotState.get(userId);
        if (botState == null){
            botState = BotState.DEFAULT;
        }

        return botState;
    }

    @Override
    public void setUsersCurrentCodeCache(int userId, CodeCache codeCache) {
        usersCodeCache.put(userId, codeCache);
    }

    @Override
    public CodeCache getUsersCurrentCodeCache(int userId) {
        CodeCache codeCache = usersCodeCache.get(userId);
        if (codeCache == null){
            codeCache = new CodeCache();
        }

        return codeCache;
    }


}
