package com.xatoxa.intercomcodesbot.bot;

import com.xatoxa.intercomcodesbot.botapi.BotState;
import com.xatoxa.intercomcodesbot.cache.CodeCache;
import com.xatoxa.intercomcodesbot.cache.UserDataCache;
import com.xatoxa.intercomcodesbot.config.BotConfig;
import com.xatoxa.intercomcodesbot.entity.*;
import com.xatoxa.intercomcodesbot.handlers.CallbackHandler;
import com.xatoxa.intercomcodesbot.handlers.Handler;
import com.xatoxa.intercomcodesbot.handlers.LocationHandler;
import com.xatoxa.intercomcodesbot.handlers.TextHandler;
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
import org.telegram.telegrambots.meta.generics.LongPollingBot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class Bot extends TelegramLongPollingBot {

    final BotConfig config;
    final LocaleMessageService msgService;

    @Autowired
    LocationHandler locationHandler;

    @Autowired
    TextHandler textHandler;

    @Autowired
    CallbackHandler callbackHandler;

    public Bot(BotConfig config, LocaleMessageService msgService){
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

    public Long getOwnerId(){
        return this.config.getOwnerId();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            if (update.getMessage().hasText()) {
                textHandler.handle(update, msgService);
            }
            else if (update.getMessage().hasLocation()) {
                locationHandler.handle(update, msgService);
            }
        }
        else if (update.hasCallbackQuery()) {
            callbackHandler.handle(update, msgService);
        }
    }
}