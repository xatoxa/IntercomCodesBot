package com.xatoxa.intercomcodesbot.cache;

import com.xatoxa.intercomcodesbot.botapi.BotState;

public interface DataCache {
    void setUsersCurrentBotState(Long userId, BotState botState);

    BotState getUsersCurrentBotState(Long userId);

    void setUsersCurrentCodeCache(Long userId, CodeCache codeCache);

    CodeCache getUsersCurrentCodeCache(Long userId);
}
