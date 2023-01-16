package com.xatoxa.intercomcodesbot.handlers;

import com.xatoxa.intercomcodesbot.botapi.BotState;
import com.xatoxa.intercomcodesbot.cache.CodeCache;
import com.xatoxa.intercomcodesbot.entity.*;
import com.xatoxa.intercomcodesbot.service.LocaleMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Component
@Slf4j
public class TextHandler extends Handler{
    @Override
    public void handle(Update update, LocaleMessageService msgService){
        BotState botState;
        String msgText = update.getMessage().getText();
        Long userId = update.getMessage().getFrom().getId();
        Long chatId = update.getMessage().getChatId();

        switch (msgText) {
            case "/start" -> {
                if (!userService.existsById(userId)) {
                    sendMessage(chatId, msgService.get("message.sendInviteRequest"),
                            getMarkup(getKeyboardRow(msgService.get("button.sendInviteRequest"), BUTTON_INVITE_REQUEST)));
                    botState = BotState.DEFAULT;
                } else {
                    User user = userService.findById(userId);
                    if (user.isEnabled()) {
                        user.setChatId(chatId);
                        try{
                            userService.save(user);
                        }catch (Exception e){
                            log.error(e.getMessage());
                        }
                        sendMessage(chatId, msgService.get("message.searchMode"));
                        sendMessage(chatId, msgService.get("message.awaitingGeoOrKey"),
                                getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)));
                        botState = BotState.SEARCH;
                    } else {
                        sendMessage(chatId, msgService.get("message.waitForConfirm"));
                        botState = BotState.DEFAULT;
                    }
                }
            }
            case "/help" -> {
                sendMessage(chatId, msgService.get("message.help"));
                botState = BotState.DEFAULT;
            }
            case "/search" -> {
                if (userService.isEnabled(userId)) {
                    sendMessage(chatId, msgService.get("message.awaitingGeoOrKey"),
                            getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)));
                    botState = BotState.SEARCH;
                } else {
                    sendMessage(chatId, msgService.get("message.notFoundSuchUser"));
                    botState = BotState.DEFAULT;
                }
            }
            case "/add" -> {
                if (userService.isEnabled(userId)) {
                    sendMessage(chatId, msgService.get("message.awaitingGeo"),
                            getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)));
                    botState = BotState.ADD;
                } else {
                    sendMessage(chatId, msgService.get("message.notFoundSuchUser"));
                    botState = BotState.DEFAULT;
                }
            }
            case "/delete" -> {
                if (userService.isEnabled(userId)) {
                    sendMessage(chatId, msgService.get("message.awaitingGeo"),
                            getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)));
                    botState = BotState.DELETE;
                } else {
                    sendMessage(chatId, msgService.get("message.notFoundSuchUser"));
                    botState = BotState.DEFAULT;
                }
            }
            case "/edit" -> {
                if (userService.isEnabled(userId)) {
                    sendMessage(chatId, msgService.get("message.awaitingGeo"),
                            getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)));
                    botState = BotState.EDIT;
                } else {
                    sendMessage(chatId, msgService.get("message.notFoundSuchUser"));
                    botState = BotState.DEFAULT;
                }
            }
            case "/all_codes" -> {
                if (userService.isEnabled(userId)) {
                    List<Home> homes = homeService.findAll();
                    if (homes.size() == 0)
                        sendMessage(chatId, msgService.get("message.notFound"));
                    else {
                        for (String sendText :
                                listToString(homes)) {
                            sendMessage(chatId, sendText);
                        }
                    }
                } else {
                    sendMessage(chatId, msgService.get("message.notFoundSuchUser"));
                }
                botState = BotState.DEFAULT;
            }
            case "/all_changes" -> {
                if (userService.isEnabled(userId)) {
                    for (String sendText :
                            listToString(userHistoryService.findAll())) {
                        sendMessage(chatId, sendText, true);
                    }
                } else {
                    sendMessage(chatId, msgService.get("message.notFoundSuchUser"));
                }
                botState = BotState.DEFAULT;
            }
            case "/my_changes" -> {
                if (userService.isEnabled(userId)) {
                    for (String sendText :
                            listToString(userHistoryService.findAllByUserId(userId))) {
                        sendMessage(chatId, sendText, true);
                    }
                    String percent = intercomCodeService.percentOfAll(userId);
                    sendMessage(chatId,
                            msgService.get("message.influence") + percent + msgService.get("message.percentage"));
                } else {
                    sendMessage(chatId, msgService.get("message.notFoundSuchUser"));
                }
                botState = BotState.DEFAULT;
            }
            case "/admin_help" -> {
                if (userService.isEnabled(userId) || bot.getOwnerId().equals(userId)) {
                    if (userService.isAdmin(userId) || bot.getOwnerId().equals(userId)) {
                        sendMessage(chatId, msgService.get("message.admin_help"));
                    } else{
                        sendMessage(chatId, msgService.get("message.notAdmin"));
                    }
                } else {
                    sendMessage(chatId, msgService.get("message.notFoundSuchUser"));
                }
                botState = BotState.DEFAULT;
            }
            case "/invite" -> {
                if (userService.isEnabled(userId) || bot.getOwnerId().equals(userId)) {
                    if (userService.isAdmin(userId) || bot.getOwnerId().equals(userId)) {
                        Long inviteCount = inviteService.countAll();
                        sendMessage(chatId, msgService.get("message.invitations") + inviteCount.toString());
                        if (inviteCount > 0) {
                            UserInvite invite = inviteService.getFirst();
                            String invId = "&" + invite.getId();
                            sendMessage(chatId,
                                    invite.getUser().toString() + "\n\n" + msgService.get("button.accept") + "?",
                                    getMarkup(
                                            getKeyboardRow(msgService.get("button.accept"), "BUTTON_ACCEPT_USR" + invId),
                                            getKeyboardRow(msgService.get("button.reject"), "BUTTON_REJECT_USR" + invId)
                                    ),
                                    true);
                        }
                    } else{
                        sendMessage(chatId, msgService.get("message.notAdmin"));
                    }
                } else {
                    sendMessage(chatId, msgService.get("message.notFoundSuchUser"));
                }
                botState = BotState.DEFAULT;
            }
            case "/admins" -> {
                if (userService.isEnabled(userId) || bot.getOwnerId().equals(userId)) {
                    if (userService.isAdmin(userId) || bot.getOwnerId().equals(userId)) {
                        sendMessage(chatId, userService.findAllByAdminToString(true));
                    } else{
                        sendMessage(chatId, msgService.get("message.notAdmin"));
                    }
                } else {
                    sendMessage(chatId, msgService.get("message.notFoundSuchUser"));
                }
                botState = BotState.DEFAULT;
            }
            case "/users" -> {
                if (userService.isEnabled(userId) || bot.getOwnerId().equals(userId)) {
                    if (userService.isAdmin(userId) || bot.getOwnerId().equals(userId)) {
                        sendMessage(chatId, userService.findAllByAdminToString(false));
                    } else{
                        sendMessage(chatId, msgService.get("message.notAdmin"));
                    }
                } else {
                    sendMessage(chatId, msgService.get("message.notFoundSuchUser"));
                }
                botState = BotState.DEFAULT;
            }
            case "/make_admin" -> {
                if (userService.isEnabled(userId) || bot.getOwnerId().equals(userId)) {
                    if (userService.isAdmin(userId) || bot.getOwnerId().equals(userId)) {
                        sendMessage(chatId, msgService.get("message.waitId"),
                                getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)));
                        botState = BotState.MAKE_ADMIN;
                    } else{
                        sendMessage(chatId, msgService.get("message.notAdmin"));
                        botState = BotState.DEFAULT;
                    }
                } else {
                    sendMessage(chatId, msgService.get("message.notFoundSuchUser"));
                    botState = BotState.DEFAULT;
                }
            }
            case "/demote_admin" -> {
                if (userService.isEnabled(userId) || bot.getOwnerId().equals(userId)) {
                    if (userService.isAdmin(userId) || bot.getOwnerId().equals(userId)) {
                        sendMessage(chatId, msgService.get("message.waitId"),
                                getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)));
                        botState = BotState.DEMOTE_ADMIN;
                    } else{
                        sendMessage(chatId, msgService.get("message.notAdmin"));
                        botState = BotState.DEFAULT;
                    }
                } else {
                    sendMessage(chatId, msgService.get("message.notFoundSuchUser"));
                    botState = BotState.DEFAULT;
                }
            }
            default -> {
                if (userDataCache.getUsersCurrentBotState(userId).equals(BotState.ADD_HOME)){
                    CodeCache codeCache = userDataCache.getUsersCurrentCodeCache(userId);
                    Home home = codeCache.getHome();
                    home.fillAddressFromMsg(msgText);
                    codeCache.setHome(home);
                    userDataCache.setUsersCurrentCodeCache(userId, codeCache);

                    sendMessage(chatId, msgService.get("message.inputEntrance"),
                            getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)));
                    botState = BotState.ADD_ENTRANCE;
                } else if (userDataCache.getUsersCurrentBotState(userId).equals(BotState.ADD_ENTRANCE)) {
                    CodeCache codeCache = userDataCache.getUsersCurrentCodeCache(userId);
                    Entrance entrance = codeCache.getEntrance();
                    entrance.setNumber(msgText);
                    entrance.setHome(codeCache.getHome());
                    codeCache.setEntrance(entrance);
                    codeCache.getHome().addEntrance(entrance);
                    userDataCache.setUsersCurrentCodeCache(userId, codeCache);

                    sendMessage(chatId, msgService.get("message.inputCode"),
                            getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)));
                    botState = BotState.ADD_CODE;
                } else if (userDataCache.getUsersCurrentBotState(userId).equals(BotState.ADD_CODE)) {
                    CodeCache codeCache = userDataCache.getUsersCurrentCodeCache(userId);
                    IntercomCode code = codeCache.getCode();
                    code.setText(msgText);
                    code.setUserId(userId);
                    code.setEntrance(codeCache.getEntrance());
                    codeCache.setCode(code);
                    codeCache.getEntrance().addCode(code);
                    userDataCache.setUsersCurrentCodeCache(userId, codeCache);

                    sendMessage(chatId, msgService.get("message.checkInput"));
                    sendLocation(chatId, codeCache.getHome().getLocation());
                    sendMessage(chatId, codeCache.toString());

                    sendMessage(
                            chatId,
                            msgService.get("message.confirmInput"),
                            getMarkup(getKeyboardRow(msgService.get("button.confirm"), BUTTON_ACCEPT_ADD),
                                    getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)));
                    botState = BotState.ADD_ACCEPT;
                } else if (userDataCache.getUsersCurrentBotState(userId).equals(BotState.EDIT_HOME)) {
                    CodeCache codeCache = userDataCache.getUsersCurrentCodeCache(userId);
                    Home home = codeCache.getHome();
                    String oldAddress = home.getAddress();
                    home.fillAddressFromMsg(msgText);
                    codeCache.setHome(home);

                    try {
                        homeService.save(home);
                        UserHistory userHistory = new UserHistory(userId, update.getMessage().getFrom().getUserName(),
                                oldAddress + " -> " +
                                        home.getAddress(), LocalDateTime.now());
                        userHistoryService.save(userHistory);
                    }catch (Exception e){
                        log.error(e.getMessage());
                        sendMessage(chatId, msgService.get("message.error") + e.getMessage());
                    }

                    sendMessage(chatId, msgService.get("message.changed") + oldAddress + " -> " +
                            home.getAddress() + msgService.get("message.editThanks"));
                    botState = BotState.DEFAULT;
                } else if (userDataCache.getUsersCurrentBotState(userId).equals(BotState.EDIT_ENTRANCE)) {
                    CodeCache codeCache = userDataCache.getUsersCurrentCodeCache(userId);
                    Entrance entrance = codeCache.getEntrance();
                    String oldAddress = entrance.getInverseAddress();
                    entrance.setNumber(msgText);
                    codeCache.setEntrance(entrance);

                    try {
                        entranceService.save(entrance);
                        UserHistory userHistory = new UserHistory(userId, update.getMessage().getFrom().getUserName(),
                                oldAddress + " -> " +
                                        entrance.getInverseAddress(), LocalDateTime.now());
                        userHistoryService.save(userHistory);

                    }catch (Exception e){
                        log.error(e.getMessage());
                        sendMessage(chatId, msgService.get("message.error") + e.getMessage());
                    }

                    sendMessage(chatId, msgService.get("message.changed") + oldAddress + " -> " +
                            entrance.getInverseAddress() + msgService.get("message.editThanks"));
                    botState = BotState.DEFAULT;
                } else if (userDataCache.getUsersCurrentBotState(userId).equals(BotState.EDIT_CODE)) {
                    CodeCache codeCache = userDataCache.getUsersCurrentCodeCache(userId);
                    IntercomCode code = codeCache.getCode();
                    String oldAddress = code.getInverseAddress();
                    code.setText(msgText);
                    code.setUserId(userId);
                    codeCache.setCode(code);

                    try {
                        intercomCodeService.save(code);
                        UserHistory userHistory = new UserHistory(
                                userId, update.getMessage().getFrom().getUserName(),
                                oldAddress + " -> " + code.getInverseAddress(), LocalDateTime.now());
                        userHistoryService.save(userHistory);

                    }catch (Exception e){
                        log.error(e.getMessage());
                        sendMessage(chatId, msgService.get("message.error") + e.getMessage());
                    }

                    sendMessage(chatId, msgService.get("message.changed") + oldAddress + " -> " +
                            code.getInverseAddress() + msgService.get("message.editThanks"));
                    botState = BotState.DEFAULT;
                } else if (userDataCache.getUsersCurrentBotState(userId).equals(BotState.SEARCH)) {
                    List<Home> homes = homeService.findAllBy(msgText);
                    if (homes.size() == 0){
                        sendMessage(chatId, msgService.get("message.notFound"));
                        botState = BotState.SEARCH;
                    }else {
                        sendMessage(chatId, msgService.get("message.selectHome"),
                                getMarkup(homes, BUTTON_SEARCH_HOME,
                                        getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)));
                        botState = BotState.SEARCH_HOME;
                    }
                    userDataCache.setUsersCurrentBotState(userId, botState);
                } else if (userDataCache.getUsersCurrentBotState(userId).equals(BotState.MAKE_ADMIN)) {
                    try {
                        User user = userService.findById(Long.valueOf(msgText));
                        user.setAdmin(true);
                        userService.save(user);
                        sendMessage(user.getChatId(), msgService.get("message.youNowAdmin"));

                        for (Long adminChatId:   //сообщение всем админам
                                userService.findAllIdByAdmin(true)) {
                            if (adminChatId.equals(userId)) continue;
                            sendMessage(adminChatId,
                                    msgService.get("message.user") +
                                            user +
                                            msgService.get("message.userToAdminForAdmins") +
                                            userService.findById(userId));
                        }

                        sendMessage(chatId, user + "\n " + msgService.get("message.userToAdmin"));
                    }catch (Exception e){
                        log.error(e.getMessage());
                        sendMessage(chatId, msgService.get("message.error") + e.getMessage());
                    }
                    botState = BotState.DEFAULT;
                    userDataCache.setUsersCurrentBotState(userId, botState);
                } else if (userDataCache.getUsersCurrentBotState(userId).equals(BotState.DEMOTE_ADMIN)) {
                    try {
                        User user = userService.findById(Long.valueOf(msgText));
                        user.setAdmin(false);
                        userService.save(user);
                        sendMessage(user.getChatId(), msgService.get("message.youDemotedAdmin"));

                        for (Long adminChatId:   //сообщение всем админам
                                userService.findAllIdByAdmin(true)) {
                            if (adminChatId.equals(userId)) continue;
                            sendMessage(adminChatId,
                                    msgService.get("message.admin") +
                                            user +
                                            msgService.get("message.adminToUserForAdmins") +
                                            userService.findById(userId));
                        }

                        sendMessage(chatId, user + "\n" + msgService.get("message.adminToUser"));
                    }catch (Exception e){
                        log.error(e.getMessage());
                        sendMessage(chatId, msgService.get("message.error") + e.getMessage());
                    }
                    botState = BotState.DEFAULT;
                    userDataCache.setUsersCurrentBotState(userId, botState);
                } else {
                    sendMessage(chatId, msgService.get("message.default"));
                    botState = userDataCache.getUsersCurrentBotState(userId);
                }
            }
        }
        userDataCache.setUsersCurrentBotState(userId, botState);
    }

    private List<String> listToString(List<?> list){
        StringBuilder stBr = new StringBuilder();
        for (Object o:
                list) {
            stBr.append("\n");
            stBr.append(o.toString());
        }

        return splitTextForMessage(stBr.toString());
    }

    private List<String> splitTextForMessage(String string){
        List<String> strings = new ArrayList<>();

        int i = 0;
        while(i < string.length()){
            strings.add(string.substring(i, Math.min(i + 4095, string.length())));
            i += 4095;
        }

        return strings;
    }
}
