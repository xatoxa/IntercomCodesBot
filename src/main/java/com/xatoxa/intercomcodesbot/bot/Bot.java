package com.xatoxa.intercomcodesbot.bot;

import com.xatoxa.intercomcodesbot.config.BotConfig;
import com.xatoxa.intercomcodesbot.entity.*;
import com.xatoxa.intercomcodesbot.handlers.CallbackHandler;
import com.xatoxa.intercomcodesbot.handlers.LocationHandler;
import com.xatoxa.intercomcodesbot.handlers.TextHandler;
import com.xatoxa.intercomcodesbot.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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

    @Autowired
    UserService userService;

    @Autowired
    GroupService groupService;

    public UserService getUserService() {
        return userService;
    }

    public GroupService getGroupService() {
        return groupService;
    }

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
                textHandler.handle(update, msgService, this);
            } else if (update.getMessage().hasLocation() && update.getMessage().isUserMessage()) {
                locationHandler.handle(update, msgService, this);
            }
        } else if (update.hasCallbackQuery()) {
            callbackHandler.handle(update, msgService, this);
        }
    }

    @Scheduled(cron = "0 33 3 * * *")
    private void userVerification(){
        List<User> users = userService.findAll();
        for (User user:
             users) {
            if (!isUserInGroups(user.getId())){
                try {
                    userService.delete(user);
                    SendMessage sendMessage = new SendMessage(user.getChatId().toString(),
                            msgService.get("message.goodbye"));
                    execute(sendMessage);
                }catch (Exception e){
                    log.error(e.getMessage());
                }
            }
        }
    }

    public boolean isUserInGroups(Long userId){
        List<Group> groups = groupService.findAll();
        boolean isInGroup = false;

        if (!groups.isEmpty()){
            for (Group group:
                    groups) {
                try {
                    ChatMember chatMember = execute(new GetChatMember(group.getId().toString(), userId));
                    if (chatMember != null
                            && (chatMember.getStatus().equals("creator")
                            || chatMember.getStatus().equals("administrator")
                            || chatMember.getStatus().equals("member")
                            || chatMember.getStatus().equals("restricted"))) {
                        isInGroup = true;
                        break;
                    }
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return isInGroup;
    }

    public String actualizeGroups(){
        List<Group> groups = groupService.findAll();
        GetChat getChat = new GetChat();

        StringBuilder response = new StringBuilder();

        for (Group group: groups){
            getChat.setChatId(group.getId());
            try {
                Chat chat = execute(getChat);
                if (!group.getName().equals(chat.getTitle())) {
                    response.append(group.getName())
                            .append(" -> ")
                            .append(chat.getTitle())
                            .append("\n");
                    group.setName(chat.getTitle());
                    groupService.save(group);
                }
            }catch (TelegramApiException e){
                log.error(e.getMessage());
                return e.getMessage();
            }
        }

        return response.toString();
    }

    public String actualizeUsers(){
        List<Group> groups = groupService.findAll();
        List<User> users = userService.findAll();
        StringBuilder response = new StringBuilder();

        if (!groups.isEmpty()){
            for (Group group:
                    groups) {
                for (User user: users){
                    try {
                        ChatMember chatMember = execute(new GetChatMember(group.getId().toString(), user.getId()));
                        if (chatMember != null
                                && (chatMember.getStatus().equals("creator")
                                || chatMember.getStatus().equals("administrator")
                                || chatMember.getStatus().equals("member")
                                || chatMember.getStatus().equals("restricted"))) {
                            boolean isChanged = false;
                            if (user.getUsername() == null || !user.getUsername().equals(chatMember.getUser().getUserName()))
                            {
                                response.append("Username (")
                                        .append(user.getId())
                                        .append("): ")
                                        .append(user.getUsername())
                                        .append(" -> ")
                                        .append(chatMember.getUser().getUserName())
                                        .append("\n");
                                user.setUsername(chatMember.getUser().getUserName());
                                isChanged = true;
                            }
                            if (user.getFirstName() == null || !user.getFirstName().equals(chatMember.getUser().getFirstName()))
                            {
                                response.append("First Name (")
                                        .append(user.getId())
                                        .append("): ")
                                        .append(user.getFirstName())
                                        .append(" -> ")
                                        .append(chatMember.getUser().getFirstName())
                                        .append("\n");
                                user.setFirstName(chatMember.getUser().getFirstName());
                                isChanged = true;
                            }
                            if (user.getLastName() == null || !user.getLastName().equals(chatMember.getUser().getLastName()))
                            {
                                response.append("Last Name (")
                                        .append(user.getId())
                                        .append("): ")
                                        .append(user.getLastName())
                                        .append(" -> ")
                                        .append(chatMember.getUser().getLastName())
                                        .append("\n");
                                user.setLastName(chatMember.getUser().getLastName());
                                isChanged = true;
                            }
                            if (isChanged) {
                                //userService.save(user);
                            }
                        }else{
                            response.append(user)
                                    .append(" будет удалён")
                                    .append("\n");
                            //userService.delete(user);
                        }
                    } catch (TelegramApiException e) {
                        log.error(e.getMessage());
                        return e.getMessage();
                    }
                }
            }
        }

        return response.toString();
    }
}
