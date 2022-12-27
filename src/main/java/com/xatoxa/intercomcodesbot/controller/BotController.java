package com.xatoxa.intercomcodesbot.controller;

import com.xatoxa.intercomcodesbot.config.BotConfig;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Controller
public class BotController extends TelegramLongPollingBot {

    final BotConfig config;

    public BotController(BotConfig config){
        this.config = config;
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

            switch (msgText){
                case "/start":
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;
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

        }
    }
}
