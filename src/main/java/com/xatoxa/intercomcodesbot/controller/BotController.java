package com.xatoxa.intercomcodesbot.controller;

import com.xatoxa.intercomcodesbot.botapi.BotState;
import com.xatoxa.intercomcodesbot.cache.CodeCache;
import com.xatoxa.intercomcodesbot.cache.UserDataCache;
import com.xatoxa.intercomcodesbot.config.BotConfig;
import com.xatoxa.intercomcodesbot.entity.Entrance;
import com.xatoxa.intercomcodesbot.entity.Home;
import com.xatoxa.intercomcodesbot.entity.IntercomCode;
import com.xatoxa.intercomcodesbot.service.EntranceService;
import com.xatoxa.intercomcodesbot.service.HomeService;
import com.xatoxa.intercomcodesbot.service.IntercomCodeService;
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

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class BotController extends TelegramLongPollingBot {
    final static String MESSAGE_HELP = """
            Здесь будет описание всех команд, когда они будут работать""";
    final static String MESSAGE_DEFAULT = """
            Ты конечно можешь мне рассказать обо всём на свете, но отвечать я буду только этим сообщением :)""";
    final static String MESSAGE_AWAITING = "Жду от тебя команду...";
    final static String MESSAGE_AWAITING_GEO = "Жду от тебя геопозицию...";
    final static String MESSAGE_AWAITING_GEO_OR_KEYWORD = "Отправь мне геопозицию или ключевое слово для поиска (название улицы или номер дома). ";
    final static String BUTTON_CANCEL = "BUTTON_CANCEL";
    final static String BUTTON_ACCEPT = "BUTTON_ACCEPT";
    final static String BUTTON_SELECT_HOME = "BUTTON_SELECT_HOME";
    final static String BUTTON_SEARCH_HOME = "BUTTON_SEARCH_HOME";
    final static String BUTTON_SEARCH_ENTRANCE = "BUTTON_SEARCH_ENTRANCE";
    final static String BUTTON_SELECT_ENTRANCE = "BUTTON_SELECT_ENTRANCE";
    final static String BUTTON_NOT_FOUND_HOME = "BUTTON_NOT_FOUND_HOME";
    final static String BUTTON_NOT_FOUND_ENTRANCE = "BUTTON_NOT_FOUND_ENTRANCE";
    final static int MAX_ENTRANCES = 10;

    final BotConfig config;

    @Autowired
    UserDataCache userDataCache;

    @Autowired
    HomeService homeService;

    @Autowired
    EntranceService entranceService;

    @Autowired
    IntercomCodeService intercomCodeService;

    public BotController(BotConfig config){
        this.config = config;

        List<BotCommand> commands = new ArrayList<>();
        commands.add(new BotCommand("/start", "Старт!"));
        commands.add(new BotCommand("/help", "Как пользоваться этим ботом?"));
        commands.add(new BotCommand("/search", "Найти код."));
        commands.add(new BotCommand("/add", "Добавить код."));
        commands.add(new BotCommand("/delete", "Удалить код."));
        commands.add(new BotCommand("/edit", "Изменить код."));
        /*commands.add(new BotCommand("/all_changes", "Действия всех пользователей."));
        commands.add(new BotCommand("/my_changes", "Действия текущего пользователя."));
        commands.add(new BotCommand("/all_codes", "Список всех кодов."));*/

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
        BotState botState;
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

    private void textHandler(Update update, long chatId, int userId){
        BotState botState;
        String msgText = update.getMessage().getText();

        switch (msgText) {
            case "/start" -> {
                sendMessage(chatId, MESSAGE_AWAITING);
                botState = BotState.DEFAULT;
            }
            case "/help" -> {
                sendMessage(chatId, MESSAGE_HELP);
                botState = BotState.DEFAULT;
            }
            case "/search" -> {
                sendMessage(chatId, "Поиск");
                sendMessage(chatId, MESSAGE_AWAITING_GEO_OR_KEYWORD, setCancelMarkup());
                botState = BotState.SEARCH;
            }
            case "/add" -> {
                sendMessage(chatId, "Добавление");
                sendMessage(chatId, MESSAGE_AWAITING_GEO, setCancelMarkup());
                botState = BotState.ADD;
            }
            case "/delete" -> {
                sendMessage(chatId, "Удаление");
                sendMessage(chatId, MESSAGE_AWAITING_GEO, setCancelMarkup());
                botState = BotState.DELETE;
            }
            case "/edit" -> {
                sendMessage(chatId, "Изменение");
                sendMessage(chatId, MESSAGE_AWAITING_GEO, setCancelMarkup());
                botState = BotState.EDIT;
            }
            default -> {
                if (userDataCache.getUsersCurrentBotState(userId).equals(BotState.ADD_HOME)){
                    //проверка на существующий дом
                    CodeCache codeCache = userDataCache.getUsersCurrentCodeCache( userId);
                    Home home = codeCache.getHome();
                    home.fillAddressFromMsg(msgText); //добавить try на неправильный ввод

                    homeService.save(home);

                    userDataCache.setUsersCurrentCodeCache(userId, codeCache);

                    sendMessage(chatId, "Введи номер подъезда", setCancelMarkup());
                    botState = BotState.ADD_ENTRANCE;
                } else if (userDataCache.getUsersCurrentBotState(userId).equals(BotState.ADD_ENTRANCE)) {
                    CodeCache codeCache = userDataCache.getUsersCurrentCodeCache(userId);
                    Entrance entrance = new Entrance();
                    entrance.setNumber(msgText);
                    entrance.setHome(codeCache.getHome());
                    codeCache.setEntrance(entrance);
                    codeCache.getHome().addEntrance(entrance);

                    entranceService.save(entrance);

                    sendMessage(chatId, "Введи код", setCancelMarkup());
                    botState = BotState.ADD_CODE;
                } else if (userDataCache.getUsersCurrentBotState(userId).equals(BotState.ADD_CODE)) {
                    CodeCache codeCache = userDataCache.getUsersCurrentCodeCache(userId);
                    IntercomCode code = new IntercomCode();
                    code.setText(msgText);
                    code.setEntrance(codeCache.getEntrance());
                    codeCache.setCode(code);
                    codeCache.getEntrance().addCode(code);

                    intercomCodeService.save(code);

                    sendMessage(chatId, "Проверь правильно ли я записал:");
                    sendLocation(chatId, codeCache.getHome().getLocation());
                    sendMessage(chatId, codeCache.toString());

                    sendMessage(chatId, "Подтверди введённые данные", setCancelAcceptMarkup()); //добавить кнопки Назад и Подтвердить
                    botState = BotState.ADD_ACCEPT;
                } else if (userDataCache.getUsersCurrentBotState(userId).equals(BotState.SEARCH)) {
                    List<Home> homes = homeService.findAllBy(msgText);
                    if (homes.size() == 0){
                        sendMessage(chatId, "К сожалению, я ничего не нашёл :(");
                        sendMessage(chatId, MESSAGE_AWAITING);
                        botState = BotState.DEFAULT;
                        //добавить кнопки "можешь посмотреть все" или "добавить код для этого дома"
                    }else {
                        sendMessage(chatId, "Выбери дом:", setHomesMarkup(homes, BUTTON_SEARCH_HOME));
                        botState = BotState.SEARCH_HOME;
                    }
                    userDataCache.setUsersCurrentBotState(userId, botState);
                } else {
                    sendMessage(chatId, MESSAGE_DEFAULT);
                    botState = userDataCache.getUsersCurrentBotState(userId);
                }
            }
        }
        userDataCache.setUsersCurrentBotState(userId, botState);
    }

    private void locationHandler(Update update, long chatId, int userId){
        BotState botState = userDataCache.getUsersCurrentBotState(userId);

        switch (botState) {
            case SEARCH -> {
                List<Home> homes = homeService.findAllBy(update.getMessage().getLocation());
                if (homes.size() == 0){
                    sendMessage(chatId, "К сожалению, я ничего не нашёл :(");
                    sendMessage(chatId, MESSAGE_AWAITING);
                    botState = BotState.DEFAULT;
                    //добавить кнопки "можешь посмотреть все" или "добавить код для этого дома"
                }else {
                    botState = BotState.SEARCH_HOME;
                    sendMessage(chatId, "Выбери дом:", setHomesMarkup(homes, BUTTON_SEARCH_HOME));
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
                    sendMessage(chatId, "Введи адрес формате Улица, дом", setCancelMarkup());
                    botState = BotState.ADD_HOME;
                }else {
                    sendMessage(chatId, "Выбери дом, для которого хочешь добавить код", setHomesMarkup(homes, BUTTON_SELECT_HOME));
                    botState = BotState.SELECT_HOME;
                }
                userDataCache.setUsersCurrentBotState(userId, botState);
            }
            case DELETE, EDIT -> {
                //ничего не найдено или
                sendMessage(chatId, "Выбери дом", setCancelMarkup());

            }
            default -> {
                sendMessage(chatId, "Используй команды, а потом уже скидывай геопозицию.");
            }
        }
    }

    private void callbackHandler(Update update){
        BotState botState;
        long messageId = update.getCallbackQuery().getMessage().getMessageId();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        int userId = update.getCallbackQuery().getFrom().getId().intValue();
        String callbackData = update.getCallbackQuery().getData();
        CodeCache codeCache = userDataCache.getUsersCurrentCodeCache(userId);

        if (callbackData.equals(BUTTON_CANCEL)) {
            IntercomCode code = codeCache.getCode();
            try {
                intercomCodeService.delete(code);
            }catch (Exception e){
                log.error("BUTTON_CANCEL " + e.getMessage());
            }
            Entrance entrance = codeCache.getEntrance();
            try {
                entrance.delCode(code);
                if (entrance.getCodes().size() == 0)
                    entranceService.delete(entrance);
            }catch (Exception e){
                log.error("BUTTON_CANCEL " + e.getMessage());
            }
            Home home = codeCache.getHome();
            try {
                home.delEntrance(entrance);
                if (home.getEntrances().size() == 0)
                    homeService.delete(home);
            }catch (Exception e){
                log.error("BUTTON_CANCEL " + e.getMessage());
            }

            editMessage(chatId, messageId, "Отменено.", null);
            sendMessage(chatId, MESSAGE_AWAITING);
            botState = BotState.DEFAULT;
            userDataCache.removeUsersCurrentCodeCache(userId);
        } else if (callbackData.equals(BUTTON_ACCEPT)) {
            editMessage(chatId, messageId, "Подтверждено. Спасибо!", null);
            sendMessage(chatId, MESSAGE_AWAITING);
            botState = BotState.DEFAULT;
            userDataCache.removeUsersCurrentCodeCache(userId);
        } else if (callbackData.equals(BUTTON_NOT_FOUND_HOME)) {
            editMessage(chatId, messageId, "Хорошо, тогда добавь новый.", null);
            sendMessage(chatId, "Введи адрес формате Улица, дом", setCancelMarkup());
            botState = BotState.ADD_HOME;
        } else if (callbackData.equals(BUTTON_NOT_FOUND_ENTRANCE)) {
            editMessage(chatId, messageId, "Хорошо, тогда добавь новый.", null);
            sendMessage(chatId, "Введи номер подъезда", setCancelMarkup());
            botState = BotState.ADD_ENTRANCE;
        } else if (callbackData.contains(BUTTON_SELECT_HOME)) {
            Home home = homeService.findById(Long.valueOf(callbackData.split("&")[1]));
            codeCache.setHome(home);
            userDataCache.setUsersCurrentCodeCache(userId, codeCache);
            if (home.getEntrances().size() == 0){
                sendMessage(chatId, "Введи номер подъезда", setCancelMarkup());
                botState = BotState.ADD_ENTRANCE;
            }else {
                sendMessage(chatId, "Выбери подъезд", setEntrancesMarkup(home.getEntrances(), BUTTON_SELECT_ENTRANCE));
                botState = BotState.SELECT_ENTRANCE;
            }
        } else if (callbackData.contains(BUTTON_SEARCH_HOME)) {
            Home home = homeService.findById(Long.valueOf(callbackData.split("&")[1]));
            codeCache.setHome(home);
            if (home.getEntrances().size() == 0){
                sendMessage(chatId, "Не могу найти подъезды у этого дома :(");
                sendMessage(chatId, MESSAGE_AWAITING);
                botState = BotState.DEFAULT;
            } else if (home.getEntrances().size() > MAX_ENTRANCES) {
                sendMessage(chatId, "Выбери подъезд", setEntrancesMarkup(home.getEntrances(), BUTTON_SEARCH_ENTRANCE));
                botState = BotState.SEARCH_ENTRANCE;
            } else {
                sendMessage(chatId, home.getAllTextCodes());
                sendMessage(chatId, MESSAGE_AWAITING);
                botState = BotState.DEFAULT;
            }
        } else if (callbackData.contains(BUTTON_SEARCH_ENTRANCE)) {
            Entrance entrance = entranceService.findById(Long.valueOf(callbackData.split("&")[1]));
            codeCache.setEntrance(entrance);
            userDataCache.setUsersCurrentCodeCache(userId, codeCache);
            if (entrance.getCodes().size() == 0){
                sendMessage(chatId, "Не могу найти коды у этого подъезда :(");
            }else {
                sendMessage(chatId, entrance.getTextCodes());
            }
            sendMessage(chatId, MESSAGE_AWAITING);
            botState = BotState.DEFAULT;
        } else if (callbackData.contains(BUTTON_SELECT_ENTRANCE)) {
            Entrance entrance = entranceService.findById(Long.valueOf(callbackData.split("&")[1]));
            codeCache.setEntrance(entrance);
            userDataCache.setUsersCurrentCodeCache(userId, codeCache);
            if (entrance.getCodes().size() == 0){
                sendMessage(chatId, "Введи код", setCancelMarkup());
            }else {
                sendMessage(
                        chatId,
                        "Если среди этих кодов есть твой, нажми Отмена" + entrance.getTextCodes(),
                        setCancelMarkup());
            }
            botState = BotState.ADD_CODE;
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

    private void editMessage(long chatId, long messageId, String text, InlineKeyboardMarkup keyboardMarkup){
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);
        message.setReplyMarkup(keyboardMarkup);

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

    private InlineKeyboardMarkup setCancelMarkup(){
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(setCancelRow());
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup setCancelAcceptMarkup() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = setCancelRow();

        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Подтвердить");
        button.setCallbackData(BUTTON_ACCEPT);
        row.add(button);
        rows.add(row);
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup setHomesMarkup(List<Home> homes, String state) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row;
        InlineKeyboardButton button;

        for (Home home:
             homes) {
            row = new ArrayList<>();
            button = new InlineKeyboardButton();
            button.setText(home.getAddress());
            button.setCallbackData(state + "&" + home.getId());
            row.add(button);
            rows.add(row);
        }

        rows.add(setCancelRow());
        if (state.equals(BUTTON_SELECT_HOME)) {
            rows.add(setNotFoundRow());
        }
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private InlineKeyboardMarkup setEntrancesMarkup(List<Entrance> entrances, String state) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row;
        InlineKeyboardButton button;

        for (Entrance entrance:
                entrances) {
            row = new ArrayList<>();
            button = new InlineKeyboardButton();
            button.setText(entrance.getNumber());
            button.setCallbackData(state + "&" + entrance.getId());
            row.add(button);
            rows.add(row);
        }

        rows.add(setCancelRow());
        if (state.equals(BUTTON_SELECT_ENTRANCE)) {
            rows.add(setNotFoundRow());
        }
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private List<InlineKeyboardButton> setNotFoundRow(){
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Добавить новый");
        button.setCallbackData(BUTTON_NOT_FOUND_HOME);
        row.add(button);

        return row;
    }

    private List<InlineKeyboardButton> setCancelRow(){
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Отмена");
        button.setCallbackData(BUTTON_CANCEL);
        row.add(button);

        return row;
    }
}
