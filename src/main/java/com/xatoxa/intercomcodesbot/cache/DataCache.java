package com.xatoxa.intercomcodesbot.cache;

import com.xatoxa.intercomcodesbot.botapi.BotState;

public interface DataCache {
    void setUsersCurrentBotState(int userId, BotState botState);

    BotState getUsersCurrentBotState(int userId);
}
