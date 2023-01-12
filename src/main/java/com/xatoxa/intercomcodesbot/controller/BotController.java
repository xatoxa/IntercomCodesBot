package com.xatoxa.intercomcodesbot.controller;

import com.xatoxa.intercomcodesbot.botapi.BotState;
import com.xatoxa.intercomcodesbot.cache.CodeCache;
import com.xatoxa.intercomcodesbot.cache.UserDataCache;
import com.xatoxa.intercomcodesbot.config.BotConfig;
import com.xatoxa.intercomcodesbot.entity.*;
import com.xatoxa.intercomcodesbot.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Location;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class BotController extends TelegramLongPollingBot {
    final static String BUTTON_CANCEL = "BUTTON_CANCEL";
    final static String BUTTON_ACCEPT_ADD = "BUTTON_ACCEPT_ADD";
    final static String BUTTON_SELECT_HOME = "BUTTON_SELECT_HOME";
    final static String BUTTON_SEARCH_HOME = "BUTTON_SEARCH_HOME";
    final static String BUTTON_SEARCH_ENTRANCE = "BUTTON_SEARCH_ENTRANCE";
    final static String BUTTON_SELECT_ENTRANCE = "BUTTON_SELECT_ENTRANCE";
    final static String BUTTON_NOT_FOUND_HOME = "BUTTON_NOT_FOUND_HOME";
    final static String BUTTON_NOT_FOUND_ENTRANCE = "BUTTON_NOT_FOUND_ENTRANCE";
    final static String BUTTON_DELETE_HOME = "BUTTON_DELETE_HOME";
    final static String BUTTON_DELETE_ENTRANCE = "BUTTON_DELETE_ENTRANCE";
    final static String BUTTON_DELETE_CODE = "BUTTON_DELETE_CODE";
    final static String BUTTON_EDIT_HOME = "BUTTON_EDIT_HOME";
    final static String BUTTON_EDIT_ENTRANCE = "BUTTON_EDIT_ENTRANCE";
    final static String BUTTON_EDIT_CODE = "BUTTON_EDIT_CODE";
    final static String BUTTON_ACCEPT_DELETE_HOME = "BUTTON_ACCEPT_DELETE_HOME";
    final static String BUTTON_ACCEPT_DELETE_ENTRANCE = "BUTTON_ACCEPT_DELETE_ENTRANCE";
    final static String BUTTON_INVITE_REQUEST = "BUTTON_INVITE_REQUEST";
    final static String BUTTON_ACCEPT_USR = "BUTTON_ACCEPT_USR";
    final static String BUTTON_REJECT_USR = "BUTTON_REJECT_USR";

    final static int MAX_ENTRANCES = 10;

    final BotConfig config;
    final LocaleMessageService msgService;

    @Autowired
    UserDataCache userDataCache;

    @Autowired
    HomeService homeService;

    @Autowired
    EntranceService entranceService;

    @Autowired
    IntercomCodeService intercomCodeService;

    @Autowired
    UserHistoryService userHistoryService;

    @Autowired
    UserService userService;

    @Autowired
    UserInviteService inviteService;

    public BotController(BotConfig config, LocaleMessageService msgService){
        this.config = config;
        this.msgService = msgService;

        List<BotCommand> commands = new ArrayList<>();
        commands.add(new BotCommand("/start", msgService.get("command.start")));
        commands.add(new BotCommand("/help",  msgService.get("command.help")));
        commands.add(new BotCommand("/search",  msgService.get("command.search")));
        commands.add(new BotCommand("/add",  msgService.get("command.add")));
        commands.add(new BotCommand("/delete",  msgService.get("command.delete")));
        commands.add(new BotCommand("/edit",  msgService.get("command.edit")));
        commands.add(new BotCommand("/all_changes",  msgService.get("command.all_changes")));
        commands.add(new BotCommand("/my_changes",  msgService.get("command.my_changes")));
        commands.add(new BotCommand("/all_codes",  msgService.get("command.all_codes")));
        commands.add(new BotCommand("/admin_help", msgService.get("command.admin_help")));

        try {
            this.execute(new SetMyCommands(commands, new BotCommandScopeDefault(), null));
        }catch (TelegramApiException e){
            log.error(e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return this.config.getBotName();
    }

    @Override
    public String getBotToken() {
        return this.config.getBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            long chatId = update.getMessage().getChatId();
            int userId = update.getMessage().getFrom().getId().intValue();

            if (update.getMessage().hasText()) {
                textHandler(update, chatId, userId);
            }
            else if (update.getMessage().hasLocation()) {
                locationHandler(update, chatId, userId);
            }
        }
        else if (update.hasCallbackQuery()) {
            callbackHandler(update);
        }
    }

    private void textHandler(Update update, long chatId, long userId){
        BotState botState;
        String msgText = update.getMessage().getText();

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
                    sendMessage(chatId, "Твой вклад в базу: " + percent + "% от общего");
                } else {
                    sendMessage(chatId, msgService.get("message.notFoundSuchUser"));
                }
                botState = BotState.DEFAULT;
            }
            case "/admin_help" -> {
                if (userService.isEnabled(userId) || config.getOwnerId().equals(userId)) {
                    if (userService.isAdmin(userId) || config.getOwnerId().equals(userId)) {
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
                if (userService.isEnabled(userId) || config.getOwnerId().equals(userId)) {
                    if (userService.isAdmin(userId) || config.getOwnerId().equals(userId)) {
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
                if (userService.isEnabled(userId) || config.getOwnerId().equals(userId)) {
                    if (userService.isAdmin(userId) || config.getOwnerId().equals(userId)) {
                        sendMessage(chatId, userService.findAllByAdmin(true));
                    } else{
                        sendMessage(chatId, msgService.get("message.notAdmin"));
                    }
                } else {
                    sendMessage(chatId, msgService.get("message.notFoundSuchUser"));
                }
                botState = BotState.DEFAULT;
            }
            case "/users" -> {
                if (userService.isEnabled(userId) || config.getOwnerId().equals(userId)) {
                    if (userService.isAdmin(userId) || config.getOwnerId().equals(userId)) {
                        sendMessage(chatId, userService.findAllByAdmin(false));
                    } else{
                        sendMessage(chatId, msgService.get("message.notAdmin"));
                    }
                } else {
                    sendMessage(chatId, msgService.get("message.notFoundSuchUser"));
                }
                botState = BotState.DEFAULT;
            }
            case "/make_admin" -> {
                if (userService.isEnabled(userId) || config.getOwnerId().equals(userId)) {
                    if (userService.isAdmin(userId) || config.getOwnerId().equals(userId)) {
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
                if (userService.isEnabled(userId) || config.getOwnerId().equals(userId)) {
                    if (userService.isAdmin(userId) || config.getOwnerId().equals(userId)) {
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

    private void locationHandler(Update update, long chatId, long userId){
        BotState botState = userDataCache.getUsersCurrentBotState(userId);

        switch (botState) {
            case SEARCH -> {
                List<Home> homes = homeService.findAllBy(update.getMessage().getLocation());
                if (homes.size() == 0){
                    sendMessage(chatId, msgService.get("message.notFound"));
                } else if (homes.size() == 1) {
                    sendMessage(chatId, homes.get(0).toString());
                } else {
                    botState = BotState.SEARCH_HOME;
                    sendMessage(chatId, msgService.get("message.selectHome"),
                            getMarkup(homes, BUTTON_SEARCH_HOME,
                                    getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)));
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
                            getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)));
                    botState = BotState.ADD_HOME;
                }else {
                    sendMessage(chatId, msgService.get("message.selectHomeExtra"),
                            getMarkup(homes, BUTTON_SELECT_HOME,
                                    getKeyboardRow(msgService.get("button.addNew"), BUTTON_NOT_FOUND_HOME),
                                    getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)));
                    botState = BotState.SELECT_HOME;
                }
                userDataCache.setUsersCurrentBotState(userId, botState);
            }
            case DELETE -> {
                List<Home> homes = homeService.findAllBy(update.getMessage().getLocation());
                if (homes.size() == 0){
                    sendMessage(chatId, msgService.get("message.notFound"));
                } else {
                    botState = BotState.DELETE_HOME;
                    sendMessage(chatId, msgService.get("message.selectHome"),
                            getMarkup(homes, BUTTON_DELETE_HOME,
                                    getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)));
                }
                userDataCache.setUsersCurrentBotState(userId, botState);
            }
            case EDIT -> {
                List<Home> homes = homeService.findAllBy(update.getMessage().getLocation());
                if (homes.size() == 0){
                    sendMessage(chatId, msgService.get("message.notFound"));
                } else {
                    botState = BotState.EDIT_HOME;
                    sendMessage(chatId, msgService.get("message.selectHome"),
                            getMarkup(homes, BUTTON_EDIT_HOME,
                                    getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)));
                }
                userDataCache.setUsersCurrentBotState(userId, botState);
            }
            default -> sendMessage(chatId, msgService.get("message.useCommands"));
        }
    }

    private void callbackHandler(Update update){
        BotState botState;
        long messageId = update.getCallbackQuery().getMessage().getMessageId();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        long userId = update.getCallbackQuery().getFrom().getId();
        String callbackData = update.getCallbackQuery().getData();
        CodeCache codeCache = userDataCache.getUsersCurrentCodeCache(userId);

        if (callbackData.equals(BUTTON_CANCEL)) {
            editMessage(chatId, messageId, msgService.get("message.cancelled"));
            sendMessage(chatId, msgService.get("message.awaitingCommand"));
            botState = BotState.DEFAULT;
            userDataCache.removeUsersCurrentCodeCache(userId);
        } else if (callbackData.equals(BUTTON_ACCEPT_ADD)) {
            Home home = codeCache.getHome();
            Entrance entrance = codeCache.getEntrance();
            IntercomCode code = codeCache.getCode();
            try {
                homeService.save(home);
                entranceService.save(entrance);
                intercomCodeService.save(code);
                UserHistory userHistory = new UserHistory(
                        userId, update.getCallbackQuery().getFrom().getUserName(), "+ " + code.getInverseAddress(), LocalDateTime.now());
                userHistoryService.save(userHistory);

                editMessage(chatId, messageId, msgService.get("message.confirm"));
            }catch (Exception e){
                log.error(e.getMessage());
                sendMessage(chatId, msgService.get("message.error") + e.getMessage());
            }

            sendMessage(chatId, msgService.get("message.addingMode"));
            botState = BotState.ADD;
            userDataCache.removeUsersCurrentCodeCache(userId);
        } else if (callbackData.equals(BUTTON_NOT_FOUND_HOME)) {
            editMessage(chatId, messageId, msgService.get("message.addNew"));
            sendMessage(chatId, msgService.get("message.inputHome"),
                    getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)));
            botState = BotState.ADD_HOME;
        } else if (callbackData.equals(BUTTON_NOT_FOUND_ENTRANCE)) {
            editMessage(chatId, messageId, msgService.get("message.addNew"));
            sendMessage(chatId, msgService.get("message.inputEntrance"),
                    getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)));
            botState = BotState.ADD_ENTRANCE;
        } else if (callbackData.contains(BUTTON_SELECT_HOME)) {
            Home home = homeService.findById(Long.valueOf(callbackData.split("&")[1]));
            codeCache.setHome(home);
            userDataCache.setUsersCurrentCodeCache(userId, codeCache);
            if (home.getEntrances().size() == 0){
                sendMessage(chatId, msgService.get("message.inputEntrance"),
                        getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)));
                botState = BotState.ADD_ENTRANCE;
            }else {
                sendMessage(chatId, msgService.get("message.selectEntrance"),
                        getMarkup(home.getEntrances(), BUTTON_SELECT_ENTRANCE,
                                getKeyboardRow(msgService.get("button.addNew"), BUTTON_NOT_FOUND_ENTRANCE),
                                getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)));
                botState = BotState.SELECT_ENTRANCE;
            }
        } else if (callbackData.contains(BUTTON_SEARCH_HOME)) {
            Home home = homeService.findById(Long.valueOf(callbackData.split("&")[1]));
            codeCache.setHome(home);
            if (home.getEntrances().size() == 0){
                sendMessage(chatId, msgService.get("message.notFoundEntrance"));
                botState = BotState.SEARCH;
            } else if (home.getEntrances().size() > MAX_ENTRANCES) {
                sendMessage(chatId, msgService.get("message.selectEntrance"),
                        getMarkup(home.getEntrances(), BUTTON_SEARCH_ENTRANCE,
                                getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)));
                botState = BotState.SEARCH_ENTRANCE;
            } else {
                sendMessage(chatId, home.toString());
                botState = BotState.SEARCH;
            }
        } else if (callbackData.contains(BUTTON_SEARCH_ENTRANCE)) {
            Entrance entrance = entranceService.findById(Long.valueOf(callbackData.split("&")[1]));
            codeCache.setEntrance(entrance);
            userDataCache.setUsersCurrentCodeCache(userId, codeCache);
            if (entrance.getCodes().size() == 0){
                sendMessage(chatId, msgService.get("message.notFoundEntrance"));
            }else {
                sendMessage(chatId, entrance.getTextCodes());
            }
            botState = BotState.SEARCH;
        } else if (callbackData.contains(BUTTON_SELECT_ENTRANCE)) {
            Entrance entrance = entranceService.findById(Long.valueOf(callbackData.split("&")[1]));
            codeCache.setEntrance(entrance);
            userDataCache.setUsersCurrentCodeCache(userId, codeCache);
            if (entrance.getCodes().size() == 0){
                sendMessage(chatId, msgService.get("message.inputCode"),
                        getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)));
            }else {
                sendMessage(
                        chatId,
                        msgService.get("message.codeAlreadyExists") + entrance.getTextCodes(),
                        getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)));
            }
            botState = BotState.ADD_CODE;
        } else if (callbackData.contains(BUTTON_DELETE_HOME)) {
            Home home = homeService.findById(Long.valueOf(callbackData.split("&")[1]));
            codeCache.setHome(home);
            userDataCache.setUsersCurrentCodeCache(userId, codeCache);
            String msgText, kbdText;
            InlineKeyboardMarkup markup;
            if (home.getEntrances().size() > 0){
                msgText = msgService.get("message.selectEntrance");
                kbdText = msgService.get("button.deleteWithEntrances");
                botState = BotState.DELETE_ENTRANCE;
                markup = getMarkup(home.getEntrances(), BUTTON_DELETE_ENTRANCE,
                        getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL),
                        getKeyboardRow(kbdText, BUTTON_ACCEPT_DELETE_HOME + "&" + home.getId()));
            } else {
                msgText = msgService.get("message.deleteHome");
                kbdText = msgService.get("button.confirmDelete");
                botState = BotState.DELETE_HOME;
                markup = getMarkup(
                        getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL),
                        getKeyboardRow(kbdText, BUTTON_ACCEPT_DELETE_HOME + "&" + home.getId()));
            }
            sendMessage(chatId, msgText + home.getAddress(), markup);
        } else if (callbackData.contains(BUTTON_DELETE_ENTRANCE)) {
            Entrance entrance = entranceService.findById(Long.valueOf(callbackData.split("&")[1]));
            String msgText, kbdText;
            InlineKeyboardMarkup markup;
            if (entrance.getCodes().size() > 0){
                msgText = msgService.get("message.selectCode");
                kbdText = msgService.get("button.deleteWithCodes");
                botState = BotState.DELETE_CODES;
                markup = getMarkup(entrance.getCodes(), BUTTON_DELETE_CODE,
                        getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL),
                        getKeyboardRow(kbdText, BUTTON_ACCEPT_DELETE_ENTRANCE + "&" + entrance.getId()));
            } else {
                msgText = msgService.get("message.deleteEntrance");
                kbdText = msgService.get("button.confirmDelete");
                botState = BotState.DELETE_ENTRANCE;
                markup = getMarkup(
                        getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL),
                        getKeyboardRow(kbdText, BUTTON_ACCEPT_DELETE_ENTRANCE + "&" + entrance.getId()));
            }
            sendMessage(chatId, msgText + entrance.getAddress(), markup);

        } else if (callbackData.contains(BUTTON_ACCEPT_DELETE_HOME)) {
            Home home = homeService.findById(Long.valueOf(callbackData.split("&")[1]));
            try {
                UserHistory userHistory = new UserHistory(userId, update.getCallbackQuery().getFrom().getUserName(),
                        "- " + home.getAddress(), LocalDateTime.now());
                homeService.delete(home);
                userHistoryService.save(userHistory);

                editMessage(chatId, messageId, msgService.get("message.deleted"));
            }catch (Exception e){
                log.error(e.getMessage());
                sendMessage(chatId, msgService.get("message.error") + e.getMessage());
            }
            sendMessage(chatId, msgService.get("message.awaitingCommand"));
            botState = BotState.DEFAULT;
        } else if (callbackData.contains(BUTTON_ACCEPT_DELETE_ENTRANCE)) {
            Entrance entrance = entranceService.findById(Long.valueOf(callbackData.split("&")[1]));
            Home home = entrance.getHome();
            try {
                UserHistory userHistory = new UserHistory(
                        userId, update.getCallbackQuery().getFrom().getUserName(),
                        "- " + entrance.getInverseAddress(), LocalDateTime.now());
                entrance.dismissHome();
                homeService.save(home);
                entranceService.delete(entrance);
                userHistoryService.save(userHistory);

                editMessage(chatId, messageId, msgService.get("message.deleted"));
            }catch (Exception e){
                log.error(e.getMessage());
                sendMessage(chatId, msgService.get("message.error") + e.getMessage());
            }
            sendMessage(chatId, msgService.get("message.awaitingCommand"));
            botState = BotState.DEFAULT;
        } else if (callbackData.contains(BUTTON_DELETE_CODE)) {
            IntercomCode code = intercomCodeService.findById(Long.valueOf(callbackData.split("&")[1]));
            Entrance entrance = code.getEntrance();
            try {
                UserHistory userHistory = new UserHistory(
                        userId, update.getCallbackQuery().getFrom().getUserName(),
                        "- " + code.getInverseAddress(), LocalDateTime.now());
                code.dismissEntrance();
                entranceService.save(entrance);
                intercomCodeService.delete(code);
                userHistoryService.save(userHistory);
                editMessage(chatId, messageId, msgService.get("message.deleted"));
            }catch (Exception e){
                log.error(e.getMessage());
                sendMessage(chatId, msgService.get("message.error") + e.getMessage());
            }
            sendMessage(chatId, msgService.get("message.awaitingCommand"));
            botState = BotState.DEFAULT;
        } else if (callbackData.contains(BUTTON_EDIT_HOME)) {
            Home home = homeService.findById(Long.valueOf(callbackData.split("&")[1]));
            codeCache.setHome(home);
            userDataCache.setUsersCurrentCodeCache(userId, codeCache);
            String msgText;
            InlineKeyboardMarkup markup;
            if (home.getEntrances().size() > 0){
                msgText = msgService.get("message.selectOrEditHome") + msgService.get("message.oldAddress");
                botState = BotState.EDIT_HOME;
                markup = getMarkup(home.getEntrances(), BUTTON_EDIT_ENTRANCE,
                        getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL));
            } else {
                msgText = msgService.get("message.editHome") + msgService.get("message.oldAddress");
                botState = BotState.EDIT_HOME;
                markup = getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL));
            }
            sendMessage(chatId, msgText + home.getAddress(), markup);
        } else if (callbackData.contains(BUTTON_EDIT_ENTRANCE)) {
            Entrance entrance = entranceService.findById(Long.valueOf(callbackData.split("&")[1]));
            codeCache.setEntrance(entrance);
            userDataCache.setUsersCurrentCodeCache(userId, codeCache);
            String msgText;
            InlineKeyboardMarkup markup;
            if (entrance.getCodes().size() > 0){
                msgText = msgService.get("message.selectOrEditEntrance") + msgService.get("message.oldAddress");
                botState = BotState.EDIT_ENTRANCE;
                markup = getMarkup(entrance.getCodes(), BUTTON_EDIT_CODE,
                        getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL));
            } else {
                msgText = msgService.get("message.editEntrance") + msgService.get("message.oldAddress");
                botState = BotState.EDIT_ENTRANCE;
                markup = getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL));
            }
            sendMessage(chatId, msgText + entrance.getAddress(), markup);
        } else if (callbackData.contains(BUTTON_EDIT_CODE)) {
            IntercomCode code = intercomCodeService.findById(Long.valueOf(callbackData.split("&")[1]));
            codeCache.setCode(code);
            userDataCache.setUsersCurrentCodeCache(userId, codeCache);
            botState = BotState.EDIT_CODE;
            sendMessage(chatId, msgService.get("message.editCode") + code.getAddress(),
                    getMarkup(getKeyboardRow(msgService.get("button.cancel"), BUTTON_CANCEL)));
        } else if (callbackData.contains(BUTTON_INVITE_REQUEST)) {
            try {
                User user = new User(userId, chatId,
                        update.getCallbackQuery().getFrom().getFirstName(),
                        update.getCallbackQuery().getFrom().getLastName(),
                        update.getCallbackQuery().getFrom().getUserName(),
                        false, false);
                userService.save(user);
                inviteService.save(new UserInvite(user));
                editMessage(chatId, messageId, msgService.get("message.inviteHBSent"));
            }catch (Exception e){
                log.error(e.getMessage());
                sendMessage(chatId, msgService.get("message.error") + e.getMessage());
            }
            botState = BotState.DEFAULT;
        } else if (callbackData.contains(BUTTON_ACCEPT_USR)) {
            try {
                UserInvite invite = inviteService.findById(Long.valueOf(callbackData.split("&")[1]));
                User user = invite.getUser();
                user.setEnabled(true);
                inviteService.delete(invite);
                userService.save(user);
                sendMessage(user.getChatId(), msgService.get("message.acceptInvite"));
                editMessage(chatId, messageId, msgService.get("message.confirm"));
            }catch (Exception e){
                log.error(e.getMessage());
                sendMessage(chatId, msgService.get("message.error") + e.getMessage());
            }
            botState = BotState.DEFAULT;
        } else if (callbackData.contains(BUTTON_REJECT_USR)) {
            try {
                UserInvite invite = inviteService.findById(Long.valueOf(callbackData.split("&")[1]));
                User user = invite.getUser();
                inviteService.delete(invite);
                userService.delete(user);
                sendMessage(user.getChatId(), msgService.get("message.rejectInvite"));
                editMessage(chatId, messageId, msgService.get("message.confirm"));
            }catch (Exception e){
                log.error(e.getMessage());
                sendMessage(chatId, msgService.get("message.error") + e.getMessage());
            }
            botState = BotState.DEFAULT;
        } else{ //обработать другие кнопки
            botState = userDataCache.getUsersCurrentBotState(userId);
        }
        userDataCache.setUsersCurrentBotState(userId, botState);
    }

    private void sendMessage(long chatId, String text){
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        executeSendMessage(message);
    }

    private void sendMessage(long chatId, String text, InlineKeyboardMarkup keyboardMarkup){
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        message.setReplyMarkup(keyboardMarkup);
        executeSendMessage(message);
    }

    private void sendMessage(long chatId, String text, boolean isMarkdown){
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        if (isMarkdown){
            message.disableWebPagePreview();
            message.setParseMode("Markdown");
        }
        executeSendMessage(message);
    }

    private void sendMessage(long chatId, String text, InlineKeyboardMarkup keyboardMarkup, boolean isMarkdown){
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        message.setReplyMarkup(keyboardMarkup);
        if (isMarkdown){
            message.disableWebPagePreview();
            message.setParseMode("Markdown");
        }
        executeSendMessage(message);
    }

    private void sendLocation(long chatId, Location location){
        SendLocation message = new SendLocation();
        message.setChatId(chatId);
        message.setLatitude(location.getLatitude());
        message.setLongitude(location.getLongitude());

        try{
            execute(message);
        }catch (TelegramApiException e){
            log.error(e.getMessage());
        }
    }

    private void editMessage(long chatId, long messageId, String text){
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);
        message.setReplyMarkup(null);

        try{
            execute(message);
        }catch (TelegramApiException e){
            log.error(e.getMessage());
        }
    }

    private void executeSendMessage(SendMessage message){
        try{
            execute(message);
        }catch (TelegramApiException e){
            log.error(e.getMessage());
        }
    }

    @SafeVarargs
    private InlineKeyboardMarkup getMarkup(List<InlineKeyboardButton> ... buttons){
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>(Arrays.asList(buttons));
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    @SafeVarargs
    private InlineKeyboardMarkup getMarkup(
            List<? extends HomeAbstract> entities,
            String state,
            List<InlineKeyboardButton> ... buttons) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row;
        InlineKeyboardButton button;

        for (HomeAbstract o:
                entities) {
            row = new ArrayList<>();
            button = new InlineKeyboardButton();
            button.setText(o.getAddress());
            button.setCallbackData(state + "&" + o.getId());

            row.add(button);
            rows.add(row);
        }

        rows.addAll(Arrays.asList(buttons));
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private List<InlineKeyboardButton> getKeyboardRow(String text, String state){
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(state);
        row.add(button);

        return row;
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
