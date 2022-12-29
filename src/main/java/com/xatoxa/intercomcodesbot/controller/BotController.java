package com.xatoxa.intercomcodesbot.controller;

import com.xatoxa.intercomcodesbot.botapi.BotState;
import com.xatoxa.intercomcodesbot.cache.CodeCache;
import com.xatoxa.intercomcodesbot.cache.UserDataCache;
import com.xatoxa.intercomcodesbot.config.BotConfig;
import com.xatoxa.intercomcodesbot.entity.Home;
import com.xatoxa.intercomcodesbot.service.EntryService;
import com.xatoxa.intercomcodesbot.service.HomeService;
import com.xatoxa.intercomcodesbot.service.IntercomCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
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
    final static String BUTTON_CANCEL = "BUTTON_CANCEL";

    final BotConfig config;

    @Autowired
    UserDataCache userDataCache;

    @Autowired
    HomeService homeService;

    @Autowired
    EntryService entryService;

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
            long userId = update.getMessage().getFrom().getId();

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
                sendMessage(chatId, MESSAGE_AWAITING);
                botState = BotState.DEFAULT;
            }
            case "/help" -> {
                sendMessage(chatId, MESSAGE_HELP);
                botState = BotState.DEFAULT;
            }
            case "/search" -> {
                sendMessage(chatId, "Поиск");
                sendMessage(chatId, MESSAGE_AWAITING_GEO, setCancelMarkup());
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
                if (userDataCache.getUsersCurrentBotState((int)userId).equals(BotState.ADD_HOME)){
                    sendMessage(chatId, "Введи номер подъезда", setCancelMarkup()); //добавить кнопку Назад
                    botState = BotState.ADD_ENTRANCE;
                } else if (userDataCache.getUsersCurrentBotState((int)userId).equals(BotState.ADD_ENTRANCE)) {
                    sendMessage(chatId, "Введи код", setCancelMarkup()); //добавить кнопку Назад
                    botState = BotState.ADD_CODE;
                } else if (userDataCache.getUsersCurrentBotState((int)userId).equals(BotState.ADD_CODE)) {
                    sendMessage(chatId, "Подтверди введённые данные", setCancelMarkup()); //добавить кнопки Назад и Подтвердить
                    botState = BotState.ADD_CODE;
                } else {
                    sendMessage(chatId, MESSAGE_DEFAULT);
                    botState = userDataCache.getUsersCurrentBotState((int) userId);
                }
            }
        }
        userDataCache.setUsersCurrentBotState((int) userId, botState);
    }

    private void locationHandler(Update update, long chatId, long userId){
        BotState botState = userDataCache.getUsersCurrentBotState((int) userId);

        switch (botState) {
            case SEARCH -> {
                //ничего не найдено или
                sendMessage(chatId, "Выбери дом:", setCancelMarkup());
            }
            case ADD -> {
                //выбери дом из имеющихся или
                sendMessage(chatId, "Введи адрес формате Улица, дом", setCancelMarkup());
                botState = BotState.ADD_HOME;
                userDataCache.setUsersCurrentBotState((int)userId, botState);
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
        long userId = update.getCallbackQuery().getFrom().getId();
        String callbackData = update.getCallbackQuery().getData();

        if (callbackData.equals(BUTTON_CANCEL)) {
            editMessage(chatId, messageId, "Отменено.", null);
            sendMessage(chatId, MESSAGE_AWAITING);
            botState = BotState.DEFAULT;
        }else{ //обработать другие кнопки
            botState = userDataCache.getUsersCurrentBotState((int) userId);
        }
        userDataCache.setUsersCurrentBotState((int) userId, botState);
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

    private void editMessage(long chatId, long messageId, String text, InlineKeyboardMarkup keyboardMarkup){
        EditMessageText messageText = new EditMessageText();
        messageText.setChatId(String.valueOf(chatId));
        messageText.setText(text);
        messageText.setMessageId((int) messageId);
        messageText.setReplyMarkup(keyboardMarkup);

        try{
            execute(messageText);
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
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Отмена");
        button.setCallbackData(BUTTON_CANCEL);
        row.add(button);
        rows.add(row);
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }
}
