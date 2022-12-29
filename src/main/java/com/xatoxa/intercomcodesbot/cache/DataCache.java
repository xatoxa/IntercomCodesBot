package com.xatoxa.intercomcodesbot.cache;

import com.xatoxa.intercomcodesbot.botapi.BotState;
import org.aopalliance.reflect.Code;

public interface DataCache {
    void setUsersCurrentBotState(int userId, BotState botState);

    BotState getUsersCurrentBotState(int userId);

    void setUsersCurrentCodeCache(int userId, CodeCache codeCache);

    CodeCache getUsersCurrentCodeCache(int userId);
}
