package com.xatoxa.intercomcodesbot.controller;

import com.xatoxa.intercomcodesbot.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Controller
@Slf4j
public class BotController extends TelegramLongPollingBot {
    final static String HELP_MESSAGE = """
            Здесь будет описание всех команд, когда они будут работать""";
    final static String DEFAULT_MESSAGE = """
            Ты конечно можешь мне рассказать обо всём на свете, но отвечать я буду только этим сообщением :)""";

    final BotConfig config;

    public BotController(BotConfig config){
        this.config = config;

        List<BotCommand> commands = new ArrayList<>();
        commands.add(new BotCommand("/start", "Старт!"));
        commands.add(new BotCommand("/help", "Как пользоваться этим ботом?"));
        commands.add(new BotCommand("/add", "Добавить код."));
        commands.add(new BotCommand("/delete", "Удалить код."));
        commands.add(new BotCommand("/edit", "Изменить код."));
        commands.add(new BotCommand("/search", "Найти код."));
        commands.add(new BotCommand("/all_changes", "Действия всех пользователей."));
        commands.add(new BotCommand("/my_changes", "Действия текущего пользователя."));
        commands.add(new BotCommand("/all_codes", "Список всех кодов."));

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
                case "/start" -> startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                case "/help" -> sendMessage(chatId, HELP_MESSAGE);
                default -> sendMessage(chatId, DEFAULT_MESSAGE);
            }
        }
    }

    private void startCommandReceived(long chatId, String name){
        StringBuilder answer = new StringBuilder();
        answer.append("Привет, ")
                .append(name)
                .append("!");

        sendMessage(chatId, answer.toString());
    }

    private void sendMessage(long chatId, String answer){
        SendMessage sendMessage = new SendMessage(String.valueOf(chatId), answer);

        try{
            execute(sendMessage);
        }catch (TelegramApiException e){
            log.error(e.getMessage());
        }
    }
}
