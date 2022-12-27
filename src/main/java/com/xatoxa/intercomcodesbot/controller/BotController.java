package com.xatoxa.intercomcodesbot.controller;

import com.xatoxa.intercomcodesbot.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Location;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Controller
@Slf4j
public class BotController extends TelegramLongPollingBot {
    final static String MESSAGE_HELP = """
            Здесь будет описание всех команд, когда они будут работать""";
    final static String MESSAGE_DEFAULT = """
            Ты конечно можешь мне рассказать обо всём на свете, но отвечать я буду только этим сообщением :)""";
    final static String MESSAGE_AWAITING = "Жду от тебя геопозицию...";
    final static String BUTTON_CANCEL = "cancel";
    final static String BUTTON_FIND = "fnd";
    final static String BUTTON_DELETE = "del";
    final static String BUTTON_ADD = "add";

    final BotConfig config;

    public BotController(BotConfig config){
        this.config = config;

        List<BotCommand> commands = new ArrayList<>();
        commands.add(new BotCommand("/start", "Старт!"));
        commands.add(new BotCommand("/help", "Как пользоваться этим ботом?"));
        /*commands.add(new BotCommand("/add", "Добавить код."));
        commands.add(new BotCommand("/delete", "Удалить код."));
        commands.add(new BotCommand("/edit", "Изменить код."));
        commands.add(new BotCommand("/search", "Найти код."));
        commands.add(new BotCommand("/all_changes", "Действия всех пользователей."));
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
        if (update.hasMessage() && update.getMessage().hasText()){
            String msgText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (msgText) {
                case "/start" -> sendMessage(chatId, MESSAGE_AWAITING, null);
                case "/help" -> sendMessage(chatId, MESSAGE_HELP, null);
                default -> sendMessage(chatId, MESSAGE_DEFAULT, null);
            }
        }
        else if (update.hasMessage() && update.getMessage().hasLocation()) {
            long chatId = update.getMessage().getChatId();

            Location location = update.getMessage().getLocation();
            StringBuilder coordinates = new StringBuilder();
            coordinates
                    .append(location.getLatitude())
                    .append(", ")
                    .append(location.getLongitude());

            sendMessage(chatId, coordinates.toString(), null);
            sendMessage(chatId, "Выбери действие:", setMainMenuMarkup(coordinates.toString()));
        }
        else if (update.hasCallbackQuery()) {
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String callbackData = update.getCallbackQuery().getData();

            if (callbackData.equals(BUTTON_CANCEL)){
                editMessage(chatId, messageId, "Отменено.", null);
                sendMessage(chatId, MESSAGE_AWAITING, null);
            } else if (callbackData.split(" ")[0].equals(BUTTON_FIND)) {
                sendMessage(chatId, "find " + callbackData.substring(4), null);
            } else if (callbackData.split(" ")[0].equals(BUTTON_ADD)) {
                sendMessage(chatId, "add " + callbackData.substring(4), null);
            } else if (callbackData.split(" ")[0].equals(BUTTON_DELETE)) {
                sendMessage(chatId, "delete " + callbackData.substring(4), null);
            }
        }
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

    private void searchCommandReceived(long chatId){
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();

        button.setText("Отмена");
        button.setCallbackData(BUTTON_CANCEL);
        row.add(button);
        rows.add(row);
        keyboardMarkup.setKeyboard(rows);

        sendMessage(chatId, "Выбери подъед из доступных:", keyboardMarkup);

    }

    private InlineKeyboardMarkup setMainMenuMarkup(String location){
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton searchButton = new InlineKeyboardButton();
        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        InlineKeyboardButton addButton = new InlineKeyboardButton();

        searchButton.setText("Найти");
        searchButton.setCallbackData(BUTTON_FIND + " " + location);
        row.add(searchButton);
        rows.add(row);

        row = new ArrayList<>();
        deleteButton.setText("Удалить");
        deleteButton.setCallbackData(BUTTON_DELETE + " "  + location);
        addButton.setText("Добавить");
        addButton.setCallbackData(BUTTON_ADD + " "  + location);
        row.add(deleteButton);
        row.add(addButton);
        rows.add(row);
        keyboardMarkup.setKeyboard(rows);

        return keyboardMarkup;
    }
}
