package com.xatoxa.intercomcodesbot.handlers;

import com.xatoxa.intercomcodesbot.bot.Bot;
import com.xatoxa.intercomcodesbot.botapi.BotState;
import com.xatoxa.intercomcodesbot.cache.CodeCache;
import com.xatoxa.intercomcodesbot.entity.Home;
import com.xatoxa.intercomcodesbot.service.LocaleMessageService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;


@Component
public class LocationHandler extends Handler{
    @Override
    public void handle(Update update, LocaleMessageService msgService, Bot bot){
        Long userId = update.getMessage().getFrom().getId();
        Long chatId = update.getMessage().getChatId();
        BotState botState = userDataCache.getUsersCurrentBotState(userId);

        switch (botState) {
            case SEARCH -> {
                List<Home> homes = homeService.findAllBy(update.getMessage().getLocation());
                if (homes.size() == 0){
                    sendMessage(chatId, msgService.get("message.notFound"), bot);
                } else if (homes.size() == 1) {
                    sendMessage(chatId, homes.get(0).toString(), bot);
                } else {
                    botState = BotState.SEARCH_HOME;
                    sendMessage(chatId, msgService.get("message.selectHome"),
                            getMarkup(homes, BUTTON_SEARCH_HOME,
                                    getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)), bot);
                }
                userDataCache.setUsersCurrentBotState(userId, botState);
            }
            case GROUP_SEARCH -> {
                List<Home> homes = homeService.findAllBy(update.getMessage().getLocation());
                if (homes.size() == 0){
                    sendMessage(chatId, msgService.get("message.notFound"), bot);
                    botState = BotState.DEFAULT;
                } else if (homes.size() == 1) {
                    sendMessage(chatId, homes.get(0).toString(), bot);
                    botState = BotState.DEFAULT;
                } else {
                    botState = BotState.GROUP_SEARCH_HOME;
                    sendMessage(chatId, msgService.get("message.selectHome"),
                            getMarkup(homes, BUTTON_GROUP_SEARCH_HOME,
                                    getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)), bot);
                }
                userDataCache.setUsersCurrentBotState(userId, botState);
            }
            case ADD -> {
                CodeCache codeCache = userDataCache.getUsersCurrentCodeCache(userId);
                Home home = new Home();
                home.fillCoordsFromLocation(update.getMessage().getLocation());
                codeCache.setHome(home);
                userDataCache.setUsersCurrentCodeCache(userId, codeCache);

                List<Home> homes = homeService.findAllBy(update.getMessage().getLocation());
                if (homes.size() == 0) {
                    sendMessage(chatId, msgService.get("message.inputHome"),
                            getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)), bot);
                    botState = BotState.ADD_HOME;
                }else {
                    sendMessage(chatId, msgService.get("message.selectHomeExtra"),
                            getMarkup(homes, BUTTON_SELECT_HOME,
                                    getKeyboardRow(msgService.get("button.addNew"), BUTTON_NOT_FOUND_HOME),
                                    getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)), bot);
                    botState = BotState.SELECT_HOME;
                }
                userDataCache.setUsersCurrentBotState(userId, botState);
            }
            case DELETE -> {
                List<Home> homes = homeService.findAllBy(update.getMessage().getLocation());
                if (homes.size() == 0){
                    sendMessage(chatId, msgService.get("message.notFound"), bot);
                } else {
                    botState = BotState.DELETE_HOME;
                    sendMessage(chatId, msgService.get("message.selectHome"),
                            getMarkup(homes, BUTTON_DELETE_HOME,
                                    getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)), bot);
                }
                userDataCache.setUsersCurrentBotState(userId, botState);
            }
            case EDIT -> {
                List<Home> homes = homeService.findAllBy(update.getMessage().getLocation());
                if (homes.size() == 0){
                    sendMessage(chatId, msgService.get("message.notFound"), bot);
                } else {
                    botState = BotState.EDIT_HOME;
                    sendMessage(chatId, msgService.get("message.selectHome"),
                            getMarkup(homes, BUTTON_EDIT_HOME,
                                    getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)), bot);
                }
                userDataCache.setUsersCurrentBotState(userId, botState);
            }
            default -> sendMessage(chatId, msgService.get("message.useCommands"), bot);
        }
    }
}
