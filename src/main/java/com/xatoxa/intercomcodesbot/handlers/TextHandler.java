package com.xatoxa.intercomcodesbot.handlers;

import com.xatoxa.intercomcodesbot.ActionType;
import com.xatoxa.intercomcodesbot.bot.Bot;
import com.xatoxa.intercomcodesbot.botapi.BotState;
import com.xatoxa.intercomcodesbot.cache.CodeCache;
import com.xatoxa.intercomcodesbot.entity.*;
import com.xatoxa.intercomcodesbot.service.GroupService;
import com.xatoxa.intercomcodesbot.service.LocaleMessageService;
import com.xatoxa.intercomcodesbot.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
@Slf4j
public class TextHandler extends Handler{
    @Override
    public void handle(Update update, LocaleMessageService msgService, Bot bot){
        UserService userService = bot.getUserService();
        GroupService groupService = bot.getGroupService();
        String msgText = update.getMessage().getText();
        Long userId = update.getMessage().getFrom().getId();
        Long chatId = update.getMessage().getChatId();
        BotState botState = userDataCache.getUsersCurrentBotState(userId);

        if (!update.getMessage().isUserMessage()){
            if (msgText.equals("/get_group_id@IntercomCodesBot") &&
                    (bot.getOwnerId().equals(userId) || userService.isAdmin(userId))){
                deleteMessage(chatId, update.getMessage().getMessageId(), bot);
                sendMessage(chatId, chatId + " " + update.getMessage().getChat().getTitle(), bot);

                botState = userDataCache.getUsersCurrentBotState(userId);
            }else if (msgText.contains("/forgive@IntercomCodesBot") &&
                    (bot.getOwnerId().equals(userId) || userService.isAdmin(userId))){
                botPatience.reset(Long.valueOf(msgText.split(" ")[1]));
                deleteMessage(chatId, update.getMessage().getMessageId(), bot);
                sendMessage(chatId, "✓", bot);

                botState = userDataCache.getUsersCurrentBotState(userId);
            } else if (msgText.contains("/search")) {
                if (userService.isEnabled(userId)) {
                    sendMessage(chatId, msgService.get("message.awaitingGeoOrKey"),
                            getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_GROUP_CANCEL  + "&" + userId)), bot);
                    botState = BotState.GROUP_SEARCH;
                } else {
                    sendMessage(chatId, userLink(update.getMessage().getFrom()) + ", " +
                            msgService.get("message.notFoundSuchUser"), bot);
                    botState = BotState.DEFAULT;
                }
                deleteMessage(chatId, update.getMessage().getMessageId(), bot);
            } else if (userDataCache.getUsersCurrentBotState(userId).equals(BotState.GROUP_SEARCH)) {
                msgText = prepareKeyword(msgText);
                sendMessage(chatId, "Поиск: " + msgText, bot);
                List<Home> homes = homeService.findAllBy(msgText);
                if (homes.size() == 0){
                    sendMessage(chatId, msgService.get("message.notFound"), bot);
                    botState = BotState.DEFAULT;
                }else {
                    sendMessage(chatId, msgService.get("message.selectHome"),
                            getMarkup(homes,
                                    BUTTON_GROUP_SEARCH_HOME + "&" + userId,
                                    getKeyboardRow(msgService.get("button.cancel"),
                                            BUTTON_GROUP_CANCEL + "&" + userId)), bot);
                    botState = BotState.GROUP_SEARCH_HOME;
                }
                userDataCache.setUsersCurrentBotState(userId, botState);
            } else if (msgText.contains("@IntercomCodesBot")){
                deleteMessage(chatId, update.getMessage().getMessageId(), bot);

                int patience = botPatience.get(userId);

                if (patience < 2){
                    sendMessage(chatId,
                            userLink(update.getMessage().getFrom()) +
                                    msgService.get("message.group.onlySearch"),
                            true, bot);
                } else if (patience == 2) {
                    if (!isGroupAdmin(userId, chatId, bot)) {
                        sendMessage(chatId,
                                userLink(update.getMessage().getFrom()) +
                                        msgService.get("message.group.tryAgain"),
                                true, bot);
                    }
                } else {
                    String sorry = "";
                    if (patience > 5)
                        sorry = "\n" + userLink(update.getMessage().getFrom()) +
                                msgService.get("message.group.saySorry");
                    if (!isGroupAdmin(userId, chatId, bot)) {
                        try {
                            muteUser(userId, chatId, patience * 4L, bot);
                            sendMessage(chatId,
                                    msgService.get("message.group.now") +
                                            userLink(update.getMessage().getFrom()) + " " +
                                            patience * 4 + msgService.get("message.group.mute") + sorry,
                                    true, bot);
                        }catch (TelegramApiException e){
                            log.error(e.getMessage());
                        }
                    }
                }
            }
        } else{
            switch (msgText) {
                case "/start" -> {
                    if (bot.isUserInGroups(userId)) {
                        sendMessage(chatId, msgService.get("message.start"), bot);
                        if (!userService.existsById(userId)) {
                            sendMessage(chatId, msgService.get("message.sendInviteRequest"),
                                    getMarkup(
                                            getKeyboardRow(msgService.get("button.sendInviteRequest"), BUTTON_INVITE_REQUEST))
                                    , bot);
                            botState = BotState.DEFAULT;
                        } else {
                            User user = userService.findById(userId);
                            if (user.isEnabled()) {
                                user.setChatId(chatId);
                                try {
                                    userService.save(user);
                                } catch (Exception e) {
                                    log.error(e.getMessage());
                                }
                                sendMessage(chatId, msgService.get("message.searchMode"), bot);
                                sendMessage(chatId, msgService.get("message.awaitingGeoOrKey"),
                                        getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)), bot);
                                botState = BotState.SEARCH;
                            } else {
                                sendMessage(chatId, msgService.get("message.waitForConfirm"), bot);
                                botState = BotState.DEFAULT;
                            }
                        }
                    } else {
                        sendMessage(chatId, msgService.get("message.notFoundInGroups"), bot);
                        botState = BotState.DEFAULT;
                    }
                }
                case "/help" -> {
                    sendMessage(chatId, msgService.get("message.help"), bot);
                    botState = BotState.DEFAULT;
                }
                case "/search" -> {
                    if (userService.isEnabled(userId)) {
                        sendMessage(chatId, msgService.get("message.awaitingGeoOrKey"),
                                getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)), bot);
                        botState = BotState.SEARCH;
                    } else {
                        sendMessage(chatId, msgService.get("message.notFoundSuchUser"), bot);
                        botState = BotState.DEFAULT;
                    }
                }
                case "/add" -> {
                    if (userService.isEnabled(userId)) {
                        sendMessage(chatId, msgService.get("message.awaitingGeoOrKey"),
                                getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)), bot);
                        botState = BotState.ADD;
                    } else {
                        sendMessage(chatId, msgService.get("message.notFoundSuchUser"), bot);
                        botState = BotState.DEFAULT;
                    }
                }
                case "/delete" -> {
                    if (userService.isEnabled(userId)) {
                        String forAdmin = "";
                        if (userService.isAdmin(userId)){
                            forAdmin = msgService.get("message.adminDeleteHome");
                        }
                        sendMessage(chatId, msgService.get("message.awaitingGeo") + forAdmin,
                                getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)), bot);
                        botState = BotState.DELETE;
                    } else {
                        sendMessage(chatId, msgService.get("message.notFoundSuchUser"), bot);
                        botState = BotState.DEFAULT;
                    }
                }
                case "/edit" -> {
                    if (userService.isEnabled(userId)) {
                        sendMessage(chatId, msgService.get("message.awaitingGeo"),
                                getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)), bot);
                        botState = BotState.EDIT;
                    } else {
                        sendMessage(chatId, msgService.get("message.notFoundSuchUser"), bot);
                        botState = BotState.DEFAULT;
                    }
                }
                case "/all_codes" -> {
                    if (userService.isEnabled(userId)) {
                        List<Home> homes = homeService.findAll();
                        if (homes.size() == 0)
                            sendMessage(chatId, msgService.get("message.notFound"), bot);
                        else {
                            for (String sendText :
                                    listToString(homes)) {
                                sendMessage(chatId, sendText, bot);
                            }
                        }
                    } else {
                        sendMessage(chatId, msgService.get("message.notFoundSuchUser"), bot);
                    }
                    botState = BotState.DEFAULT;
                }
                case "/all_changes" -> {
                    if (userService.isEnabled(userId)) {
                        for (String sendText :
                                listToString(userHistoryService.findAllByActionTypeNotSearch())) {
                            sendMessage(chatId, sendText, true, bot);
                        }
                    } else {
                        sendMessage(chatId, msgService.get("message.notFoundSuchUser"), bot);
                    }
                    botState = BotState.DEFAULT;
                }
                case "/my_changes" -> {
                    if (userService.isEnabled(userId)) {
                        for (String sendText :
                                listToString(userHistoryService.findAllByUserId(userId))) {
                            sendMessage(chatId, sendText, true, bot);
                        }
                        String percent = intercomCodeService.percentOfAll(userId);
                        sendMessage(chatId,
                                msgService.get("message.influence") + percent + msgService.get("message.percentage")
                                , bot);
                    } else {
                        sendMessage(chatId, msgService.get("message.notFoundSuchUser"), bot);
                    }
                    botState = BotState.DEFAULT;
                }
                case "/admin_help" -> {
                    if (userService.isEnabled(userId) || bot.getOwnerId().equals(userId)) {
                        if (userService.isAdmin(userId) || bot.getOwnerId().equals(userId)) {
                            sendMessage(chatId, msgService.get("message.admin_help"), bot);
                        } else{
                            sendMessage(chatId, msgService.get("message.notAdmin"), bot);
                        }
                    } else {
                        sendMessage(chatId, msgService.get("message.notFoundSuchUser"), bot);
                    }
                    botState = BotState.DEFAULT;
                }
                case "/invite" -> {
                    if (userService.isEnabled(userId) || bot.getOwnerId().equals(userId)) {
                        if (userService.isAdmin(userId) || bot.getOwnerId().equals(userId)) {
                            Long inviteCount = inviteService.countAll();
                            sendMessage(chatId, msgService.get("message.invitations") + inviteCount.toString(), bot);
                            if (inviteCount > 0) {
                                UserInvite invite = inviteService.getFirst();
                                String invId = "&" + invite.getId();
                                sendMessage(chatId,
                                        invite.getUser().toString() + "\n\n" + msgService.get("button.accept") + "?",
                                        getMarkup(
                                                getKeyboardRow(msgService.get("button.accept"), "BUTTON_ACCEPT_USR" + invId),
                                                getKeyboardRow(msgService.get("button.reject"), "BUTTON_REJECT_USR" + invId)
                                        ),
                                        true, bot);
                            }
                        } else{
                            sendMessage(chatId, msgService.get("message.notAdmin"), bot);
                        }
                    } else {
                        sendMessage(chatId, msgService.get("message.notFoundSuchUser"), bot);
                    }
                    botState = BotState.DEFAULT;
                }
                case "/admins" -> {
                    if (userService.isEnabled(userId) || bot.getOwnerId().equals(userId)) {
                        if (userService.isAdmin(userId) || bot.getOwnerId().equals(userId)) {
                            sendMessage(chatId, userService.findAllByAdminToString(true), bot);
                        } else{
                            sendMessage(chatId, msgService.get("message.notAdmin"), bot);
                        }
                    } else {
                        sendMessage(chatId, msgService.get("message.notFoundSuchUser"), bot);
                    }
                    botState = BotState.DEFAULT;
                }
                case "/users" -> {
                    if (userService.isEnabled(userId) || bot.getOwnerId().equals(userId)) {
                        if (userService.isAdmin(userId) || bot.getOwnerId().equals(userId)) {
                            sendMessage(chatId, userService.findAllByAdminToString(false), bot);
                        } else{
                            sendMessage(chatId, msgService.get("message.notAdmin"), bot);
                        }
                    } else {
                        sendMessage(chatId, msgService.get("message.notFoundSuchUser"), bot);
                    }
                    botState = BotState.DEFAULT;
                }
                case "/make_admin" -> {
                    if (userService.isEnabled(userId) || bot.getOwnerId().equals(userId)) {
                        if (userService.isAdmin(userId) || bot.getOwnerId().equals(userId)) {
                            sendMessage(chatId, msgService.get("message.waitId"),
                                    getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)), bot);
                            botState = BotState.MAKE_ADMIN;
                        } else{
                            sendMessage(chatId, msgService.get("message.notAdmin"), bot);
                            botState = BotState.DEFAULT;
                        }
                    } else {
                        sendMessage(chatId, msgService.get("message.notFoundSuchUser"), bot);
                        botState = BotState.DEFAULT;
                    }
                }
                case "/demote_admin" -> {
                    if (userService.isEnabled(userId) || bot.getOwnerId().equals(userId)) {
                        if (userService.isAdmin(userId) || bot.getOwnerId().equals(userId)) {
                            sendMessage(chatId, msgService.get("message.waitId"),
                                    getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)), bot);
                            botState = BotState.DEMOTE_ADMIN;
                        } else{
                            sendMessage(chatId, msgService.get("message.notAdmin"), bot);
                            botState = BotState.DEFAULT;
                        }
                    } else {
                        sendMessage(chatId, msgService.get("message.notFoundSuchUser"), bot);
                        botState = BotState.DEFAULT;
                    }
                }
                case "/delete_user" -> {
                    if (userService.isEnabled(userId) || bot.getOwnerId().equals(userId)) {
                        if (userService.isAdmin(userId) || bot.getOwnerId().equals(userId)) {
                            sendMessage(chatId, msgService.get("message.waitId"),
                                    getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)), bot);
                            botState = BotState.DELETE_USER;
                        } else{
                            sendMessage(chatId, msgService.get("message.notAdmin"), bot);
                            botState = BotState.DEFAULT;
                        }
                    } else {
                        sendMessage(chatId, msgService.get("message.notFoundSuchUser"), bot);
                        botState = BotState.DEFAULT;
                    }
                }
                case "/add_group" -> {
                    if (userService.isEnabled(userId) || bot.getOwnerId().equals(userId)) {
                        if (userService.isAdmin(userId) || bot.getOwnerId().equals(userId)) {
                            sendMessage(chatId, msgService.get("message.waitIdGroup"),
                                    getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)), bot);
                            botState = BotState.ADD_CHAT;
                        } else{
                            sendMessage(chatId, msgService.get("message.notAdmin"), bot);
                            botState = BotState.DEFAULT;
                        }
                    } else {
                        sendMessage(chatId, msgService.get("message.notFoundSuchUser"), bot);
                        botState = BotState.DEFAULT;
                    }
                }
                case "/delete_group" -> {
                    if (userService.isEnabled(userId) || bot.getOwnerId().equals(userId)) {
                        if (userService.isAdmin(userId) || bot.getOwnerId().equals(userId)) {
                            sendMessage(chatId, msgService.get("message.waitIdGroup"),
                                    getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)), bot);
                            botState = BotState.DELETE_CHAT;
                        } else{
                            sendMessage(chatId, msgService.get("message.notAdmin"), bot);
                            botState = BotState.DEFAULT;
                        }
                    } else {
                        sendMessage(chatId, msgService.get("message.notFoundSuchUser"), bot);
                        botState = BotState.DEFAULT;
                    }
                }
                case "/groups" -> {
                    if (userService.isEnabled(userId) || bot.getOwnerId().equals(userId)) {
                        if (userService.isAdmin(userId) || bot.getOwnerId().equals(userId)) {
                            sendMessage(chatId, groupService.findAllToString(), bot);
                            botState = BotState.DELETE_CHAT;
                        } else{
                            sendMessage(chatId, msgService.get("message.notAdmin"), bot);
                            botState = BotState.DEFAULT;
                        }
                    } else {
                        sendMessage(chatId, msgService.get("message.notFoundSuchUser"), bot);
                        botState = BotState.DEFAULT;
                    }
                }
                case "/searchStats" -> {
                    if (userService.isEnabled(userId) || bot.getOwnerId().equals(userId)) {
                        if (userService.isAdmin(userId) || bot.getOwnerId().equals(userId)) {
                            sendMessage(chatId, msgService.get("message.statsChoice"), bot);
                        } else{
                            sendMessage(chatId, msgService.get("message.notAdmin"), bot);
                        }
                    } else {
                        sendMessage(chatId, msgService.get("message.notFoundSuchUser"), bot);
                    }
                    botState = BotState.DEFAULT;
                }
                case "/searchStatsToday" -> {
                    if (userService.isEnabled(userId) || bot.getOwnerId().equals(userId)) {
                        if (userService.isAdmin(userId) || bot.getOwnerId().equals(userId)) {
                            List<UserHistory> userHistoryListToday = userHistoryService.findAllByActionTypeIsSearchToday();
                            if (userHistoryListToday.isEmpty())
                                sendMessage(chatId, msgService.get("message.notFound"), true, bot);
                            for (String sendText :
                                    listToString(userHistoryListToday)) {
                                sendMessage(chatId, sendText, true, bot);
                            }
                        } else{
                            sendMessage(chatId, msgService.get("message.notAdmin"), bot);
                        }
                    } else {
                        sendMessage(chatId, msgService.get("message.notFoundSuchUser"), bot);
                    }
                    botState = BotState.DEFAULT;
                }
                case "/searchStatsAll" -> {
                    if (userService.isEnabled(userId) || bot.getOwnerId().equals(userId)) {
                        if (userService.isAdmin(userId) || bot.getOwnerId().equals(userId)) {
                            for (String sendText :
                                    listToString(userHistoryService.findAllByActionTypeIsSearch())) {
                                sendMessage(chatId, sendText, true, bot);
                            }
                        } else{
                            sendMessage(chatId, msgService.get("message.notAdmin"), bot);
                        }
                    } else {
                        sendMessage(chatId, msgService.get("message.notFoundSuchUser"), bot);
                    }
                    botState = BotState.DEFAULT;
                }
                case "/searchStatsTodayInFile" -> {
                    if (userService.isEnabled(userId) || bot.getOwnerId().equals(userId)) {
                        if (userService.isAdmin(userId) || bot.getOwnerId().equals(userId)) {
                            List<UserHistory> userHistoryListToday = userHistoryService.findAllByActionTypeIsSearchToday();
                            String filePath = "intercom/files/statsBy" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss")) + ".txt";
                            writeListToFile(userHistoryListToday, filePath);
                            sendFile(chatId, filePath, bot);
                            File file = new File(filePath);
                            file.delete();
                        } else{
                            sendMessage(chatId, msgService.get("message.notAdmin"), bot);
                        }
                    } else {
                        sendMessage(chatId, msgService.get("message.notFoundSuchUser"), bot);
                    }
                    botState = BotState.DEFAULT;
                }
                case "/searchStatsAllInFile" -> {
                    if (userService.isEnabled(userId) || bot.getOwnerId().equals(userId)) {
                        if (userService.isAdmin(userId) || bot.getOwnerId().equals(userId)) {
                            List<UserHistory> userHistoryListToday = userHistoryService.findAllByActionTypeIsSearch();
                            String filePath = "intercom" + File.separator + "files" + File.separator + "statsAll.txt";
                            writeListToFile(userHistoryListToday, filePath);
                            sendFile(chatId, filePath, bot);
                            File file = new File(filePath);
                            file.delete();
                        } else{
                            sendMessage(chatId, msgService.get("message.notAdmin"), bot);
                        }
                    } else {
                        sendMessage(chatId, msgService.get("message.notFoundSuchUser"), bot);
                    }
                    botState = BotState.DEFAULT;
                }
                default -> {
                    if (userDataCache.getUsersCurrentBotState(userId).equals(BotState.ADD_HOME)){
                        CodeCache codeCache = userDataCache.getUsersCurrentCodeCache(userId);
                        Home home = codeCache.getHome();
                        home.fillAddressFromMsg(msgText);
                        codeCache.setHome(home);
                        userDataCache.setUsersCurrentCodeCache(userId, codeCache);

                        sendMessage(chatId, msgService.get("message.inputEntrance"),
                                getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)), bot);
                        botState = BotState.ADD_ENTRANCE;
                    } else if (userDataCache.getUsersCurrentBotState(userId).equals(BotState.ADD)){
                        msgText = prepareKeyword(msgText);
                        List<Home> homes = homeService.findAllBy(msgText);
                        if (homes.size() == 0) {
                            sendMessage(chatId, msgService.get("message.inputHome"),
                                    getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)), bot);
                            botState = BotState.ADD_HOME;
                        }else {
                            sendMessage(chatId, msgService.get("message.selectHomeExtra"),
                                    getMarkup(homes, BUTTON_SELECT_HOME,
                                            getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)), bot);
                            botState = BotState.SELECT_HOME;
                        }
                        userDataCache.setUsersCurrentBotState(userId, botState);

                    } else if (userDataCache.getUsersCurrentBotState(userId).equals(BotState.ADD_ENTRANCE)) {
                        CodeCache codeCache = userDataCache.getUsersCurrentCodeCache(userId);
                        Entrance entrance = codeCache.getEntrance();
                        entrance.setNumber(msgText);
                        entrance.setHome(codeCache.getHome());
                        codeCache.setEntrance(entrance);
                        codeCache.getHome().addEntrance(entrance);
                        userDataCache.setUsersCurrentCodeCache(userId, codeCache);

                        sendMessage(chatId, msgService.get("message.inputCode"),
                                getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)), bot);
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

                        sendMessage(chatId, msgService.get("message.checkInput"), bot);
                        sendLocation(chatId, codeCache.getHome().getLocation(), bot);
                        sendMessage(chatId, codeCache.toString(), bot);

                        sendMessage(
                                chatId,
                                msgService.get("message.confirmInput"),
                                getMarkup(getKeyboardRow(msgService.get("button.confirm"), BUTTON_ACCEPT_ADD),
                                        getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)), bot);
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
                                            home.getAddress(), ActionType.MODIFICATE, LocalDateTime.now());
                            userHistoryService.save(userHistory);
                        }catch (Exception e){
                            log.error(e.getMessage());
                            sendMessage(chatId, msgService.get("message.error") + e.getMessage(), bot);
                        }

                        sendMessage(chatId, msgService.get("message.changed") + oldAddress + " -> " +
                                home.getAddress() + msgService.get("message.editThanks"), bot);
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
                                            entrance.getInverseAddress(), ActionType.MODIFICATE, LocalDateTime.now());
                            userHistoryService.save(userHistory);

                        }catch (Exception e){
                            log.error(e.getMessage());
                            sendMessage(chatId, msgService.get("message.error") + e.getMessage(), bot);
                        }

                        sendMessage(chatId, msgService.get("message.changed") + oldAddress + " -> " +
                                entrance.getInverseAddress() + msgService.get("message.editThanks"), bot);
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
                                    oldAddress + " -> " + code.getInverseAddress(), ActionType.MODIFICATE, LocalDateTime.now());
                            userHistoryService.save(userHistory);

                        }catch (Exception e){
                            log.error(e.getMessage());
                            sendMessage(chatId, msgService.get("message.error") + e.getMessage(), bot);
                        }

                        sendMessage(chatId, msgService.get("message.changed") + oldAddress + " -> " +
                                code.getInverseAddress() + msgService.get("message.editThanks"), bot);
                        botState = BotState.DEFAULT;
                    } else if (userDataCache.getUsersCurrentBotState(userId).equals(BotState.SEARCH)) {
                        msgText = prepareKeyword(msgText);
                        sendMessage(chatId, "Поиск: " + msgText, bot);
                        List<Home> homes = homeService.findAllBy(msgText);

                        //сохранение поискового запроса в историю
                        StringBuilder historyHomes = new StringBuilder();
                        Iterator<Home> stringIterator = homes.iterator();
                        while(stringIterator.hasNext()){
                            historyHomes.append(stringIterator.next().getAddress());

                            if(stringIterator.hasNext()) {
                                historyHomes.append("; ");
                            }
                        }
                        UserHistory userHistory = new UserHistory(
                                userId, update.getMessage().getFrom().getUserName(),
                                "Ввод: \"" + msgText + "\" | Получено " + homes.size() + ": " + historyHomes,
                                ActionType.SEARCH, LocalDateTime.now());
                        userHistoryService.save(userHistory);

                        if (homes.size() == 0){
                            sendMessage(chatId, msgService.get("message.notFound"), bot);
                            botState = BotState.SEARCH;
                        }else {
                            sendMessage(chatId, msgService.get("message.selectHome"),
                                    getMarkup(homes, BUTTON_SEARCH_HOME,
                                            getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)), bot);
                            botState = BotState.SEARCH_HOME;
                        }
                        userDataCache.setUsersCurrentBotState(userId, botState);
                    } else if (userDataCache.getUsersCurrentBotState(userId).equals(BotState.MAKE_ADMIN)) {
                        try {
                            User user = userService.findById(Long.valueOf(msgText));
                            user.setAdmin(true);
                            userService.save(user);
                            sendMessage(user.getChatId(), msgService.get("message.youNowAdmin"), bot);

                            for (Long adminChatId:   //сообщение всем админам
                                    userService.findAllIdByAdmin(true)) {
                                if (adminChatId.equals(userId)) continue;
                                sendMessage(adminChatId,
                                        msgService.get("message.user") +
                                                user +
                                                msgService.get("message.userToAdminForAdmins") +
                                                userService.findById(userId), bot);
                            }

                            sendMessage(chatId, user + "\n " + msgService.get("message.userToAdmin"), bot);
                        }catch (Exception e){
                            log.error(e.getMessage());
                            sendMessage(chatId, msgService.get("message.error") + e.getMessage(), bot);
                        }
                        botState = BotState.DEFAULT;
                        userDataCache.setUsersCurrentBotState(userId, botState);
                    } else if (userDataCache.getUsersCurrentBotState(userId).equals(BotState.DEMOTE_ADMIN)) {
                        try {
                            User user = userService.findById(Long.valueOf(msgText));
                            user.setAdmin(false);
                            userService.save(user);
                            sendMessage(user.getChatId(), msgService.get("message.youDemotedAdmin"), bot);

                            for (Long adminChatId:   //сообщение всем админам
                                    userService.findAllIdByAdmin(true)) {
                                if (adminChatId.equals(userId)) continue;
                                sendMessage(adminChatId,
                                        msgService.get("message.admin") +
                                                user +
                                                msgService.get("message.adminToUserForAdmins") +
                                                userService.findById(userId), bot);
                            }

                            sendMessage(chatId, user + "\n" + msgService.get("message.adminToUser"), bot);
                        }catch (Exception e){
                            log.error(e.getMessage());
                            sendMessage(chatId, msgService.get("message.error") + e.getMessage(), bot);
                        }
                        botState = BotState.DEFAULT;
                        userDataCache.setUsersCurrentBotState(userId, botState);
                    } else if (userDataCache.getUsersCurrentBotState(userId).equals(BotState.DELETE_USER)) {
                        try {
                            User user = userService.findById(Long.valueOf(msgText));
                            userService.delete(user);
                            sendMessage(user.getChatId(), msgService.get("message.youDeletedUser"), bot);

                            for (Long adminChatId:   //сообщение всем админам
                                    userService.findAllIdByAdmin(true)) {
                                if (adminChatId.equals(userId)) continue;
                                sendMessage(adminChatId,
                                        msgService.get("message.user") +
                                                user +
                                                msgService.get("message.adminDeleteUser") +
                                                userService.findById(userId), bot);
                            }

                            sendMessage(chatId, user + "\n" + msgService.get("message.deleteUser"), bot);
                        }catch (Exception e){
                            log.error(e.getMessage());
                            sendMessage(chatId, msgService.get("message.error") + e.getMessage(), bot);
                        }
                        botState = BotState.DEFAULT;
                        userDataCache.setUsersCurrentBotState(userId, botState);
                    } else if (userDataCache.getUsersCurrentBotState(userId).equals(BotState.ADD_CHAT)) {
                        try {
                            Group group = new Group(Long.valueOf(msgText.split(" ")[0]),
                                    msgText.substring(msgText.indexOf(" ")));
                            groupService.save(group);
                            sendMessage(chatId,  msgService.get("message.addGroup") + group, bot);
                        }catch (Exception e){
                            log.error(e.getMessage());
                            sendMessage(chatId, msgService.get("message.error") + e.getMessage(), bot);
                        }
                        botState = BotState.DEFAULT;
                        userDataCache.setUsersCurrentBotState(userId, botState);
                    } else if (userDataCache.getUsersCurrentBotState(userId).equals(BotState.DELETE_CHAT)) {
                        try {
                            Group group = groupService.findById(Long.valueOf(msgText));
                            groupService.delete(group);
                            sendMessage(chatId, msgService.get("message.deleteGroup") + group, bot);
                        }catch (Exception e){
                            log.error(e.getMessage());
                            sendMessage(chatId, msgService.get("message.error") + e.getMessage(), bot);
                        }
                        botState = BotState.DEFAULT;
                        userDataCache.setUsersCurrentBotState(userId, botState);
                    } else if (userDataCache.getUsersCurrentBotState(userId).equals(BotState.DELETE) &&
                            (userService.isAdmin(userId) || bot.getOwnerId().equals(userId) )) {
                        msgText = prepareKeyword(msgText);
                        List<Home> homes = homeService.findAllBy(msgText);
                        if (homes.size() == 0){
                            sendMessage(chatId, msgService.get("message.notFound"), bot);
                            botState = BotState.DEFAULT;
                        }else {
                            sendMessage(chatId, msgService.get("message.selectHome"),
                                    getMarkup(homes, BUTTON_DELETE_HOME,
                                            getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)), bot);
                            botState = BotState.DELETE_HOME;
                        }
                        userDataCache.setUsersCurrentBotState(userId, botState);
                    } else {
                        if (update.getMessage().isUserMessage()) {
                            sendMessage(chatId, msgService.get("message.default"), bot);
                        }
                        botState = userDataCache.getUsersCurrentBotState(userId);
                    }
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

    private String userLink(org.telegram.telegrambots.meta.api.objects.User user){
        String link = user.getFirstName() + " " +
                (user.getLastName() == null ? "" : user.getLastName() + " ");
        return user.getUserName() == null ?
                "<a href=\"tg://user?id=" + user.getId() + "\">" + link + "</a>"
                : "@" + user.getUserName();
    }

    private String prepareKeyword(String keyword){
        StringBuilder stB = new StringBuilder(keyword.toLowerCase());

        replaceAll(stB, "[/\\\\\\\\]", "к");
        replaceAll(stB, "\\p{Punct}", "");
        replaceAll(stB, ".*?\\bмп\\b.*?", "московский");
        replaceAll(stB, ".*?\\bхользы\\b.*?", "хользунова");
        replaceAll(stB, ".*?\\bхз\\b.*?", "хользунова");
        replaceAll(stB, ".*?\\bострова\\b.*?", "хользунова");
        replaceAll(stB, ".*?\\bшишки\\b.*?", "шишкова");
        replaceAll(stB, ".*?\\bармия\\b.*?", "армии");
        replaceAll(stB, ".*?\\bпоебень\\b.*?", "армии");
        replaceAll(stB, ".*?\\bлизюки\\b.*?", "лизюкова");
        replaceAll(stB, ".*?\\bлизуны\\b.*?", "лизюкова");
        replaceAll(stB, ".*?\\bжуки\\b.*?", "жукова");
        replaceAll(stB, "\\bулица\\b", "");
        replaceAll(stB, "\\bпроспект\\b", "");
        replaceAll(stB, "\\bпереулок\\b", "");
        replaceAll(stB, "\\s+", " ");

        return stB.toString().trim();
    }

    private static void replaceAll(StringBuilder sb, String find, String replace){
        Pattern p = Pattern.compile(find);
        Matcher m = p.matcher(sb);

        int startIndex = 0;

        while (m.find(startIndex)){
            sb.replace(m.start(), m.end(), replace);

            startIndex = m.start() + replace.length();
        }
    }

    private static void writeListToFile(List<UserHistory> list, String filePath) {
        File file = new File(filePath);
        try {
            if (file.createNewFile()) {
                log.info("Файл создан: " + file.getName());
            } else {
                log.error("Файл уже существует: " + file.getName());
            }
        } catch (IOException e) {
            log.error("Ошибка при создании файла: " + e.getMessage());
            e.printStackTrace();
            return; // Выходим из метода, если не удалось создать файл
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (UserHistory item : list) {
                writer.write(item.toString());
                writer.newLine();
            }
            log.info("записал в " + filePath);
        } catch (IOException e) {
            log.error("Ошибка при записи файла: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
