package com.xatoxa.intercomcodesbot.handlers;

import com.xatoxa.intercomcodesbot.bot.Bot;
import com.xatoxa.intercomcodesbot.cache.BotPatienceCache;
import com.xatoxa.intercomcodesbot.cache.UserDataCache;
import com.xatoxa.intercomcodesbot.entity.HomeAbstract;
import com.xatoxa.intercomcodesbot.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.ChatPermissions;
import org.telegram.telegrambots.meta.api.objects.Location;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public abstract class Handler {
    final static String BUTTON_CANCEL = "BUTTON_CANCEL";
    final static String BUTTON_GROUP_CANCEL = "BUTTON_GROUP_CANCEL";
    final static String BUTTON_ACCEPT_ADD = "BUTTON_ACCEPT_ADD";
    final static String BUTTON_SELECT_HOME = "BUTTON_SELECT_HOME";
    final static String BUTTON_SEARCH_HOME = "BUTTON_SEARCH_HOME";
    final static String BUTTON_SEARCH_ENTRANCE = "BUTTON_SEARCH_ENTRANCE";
    final static String BUTTON_GROUP_SEARCH_HOME = "BUTTON_GROUP_SEARCH_HOME";
    final static String BUTTON_GROUP_SEARCH_ENTRANCE = "BUTTON_GROUP_SEARCH_ENTRANCE";
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

    @Autowired
    UserDataCache userDataCache;

    @Autowired
    BotPatienceCache botPatience;

    @Autowired
    HomeService homeService;

    @Autowired
    EntranceService entranceService;

    @Autowired
    IntercomCodeService intercomCodeService;

    @Autowired
    UserHistoryService userHistoryService;

    @Autowired
    UserInviteService inviteService;

    public abstract void handle(Update update, LocaleMessageService msgService, Bot bot);

    private void executeSendMessage(SendMessage message, Bot bot){
        try{
            bot.execute(message);
        }catch (TelegramApiException e){
            log.error(e.getMessage());
        }
    }

    protected void sendMessage(long chatId, String text, Bot bot){
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        executeSendMessage(message, bot);
    }

    protected void sendMessage(long chatId, String text, InlineKeyboardMarkup keyboardMarkup, Bot bot){
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        message.setReplyMarkup(keyboardMarkup);
        executeSendMessage(message, bot);
    }

    protected void sendMessage(long chatId, String text, boolean isMarkdown, Bot bot){
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        if (isMarkdown){
            message.disableWebPagePreview();
            message.setParseMode("Markdown");
            message.enableMarkdown(true);
        }
        executeSendMessage(message, bot);
    }

    protected void sendMessage(long chatId, String text, InlineKeyboardMarkup keyboardMarkup, boolean isMarkdown, Bot bot){
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        message.setReplyMarkup(keyboardMarkup);
        if (isMarkdown){
            message.disableWebPagePreview();
            message.enableMarkdown(true);
        }
        executeSendMessage(message, bot);
    }

    protected void sendLocation(long chatId, Location location, Bot bot){
        SendLocation message = new SendLocation();
        message.setChatId(chatId);
        message.setLatitude(location.getLatitude());
        message.setLongitude(location.getLongitude());

        try{
            bot.execute(message);
        }catch (TelegramApiException e){
            log.error(e.getMessage());
        }
    }

    protected void editMessage(long chatId, long messageId, String text, Bot bot){
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);
        message.setReplyMarkup(null);

        try{
            bot.execute(message);
        }catch (TelegramApiException e){
            log.error(e.getMessage());
        }
    }

    @SafeVarargs
    protected final InlineKeyboardMarkup getMarkup(List<InlineKeyboardButton>... buttons){
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>(Arrays.asList(buttons));
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    @SafeVarargs
    protected final InlineKeyboardMarkup getMarkup(
            List<? extends HomeAbstract> entities,
            String state,
            List<InlineKeyboardButton>... buttons) {
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

    protected List<InlineKeyboardButton> getKeyboardRow(String text, String state){
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(state);
        row.add(button);

        return row;
    }

    protected void deleteMessage(long chatId, long messageId,  Bot bot){
        DeleteMessage deleteMessage = new DeleteMessage(String.valueOf(chatId), (int)messageId);

        try{
            bot.execute(deleteMessage);
        }catch (TelegramApiException e){
            log.error(e.getMessage());
        }
    }

    protected void muteUser(Long userId, Long chatId, Long minutes, Bot bot) throws TelegramApiException {
        ChatPermissions permissions = ChatPermissions.builder()
                .canInviteUsers(false)
                .canChangeInfo(false)
                .canAddWebPagePreviews(false)
                .canPinMessages(false)
                .canSendMediaMessages(false)
                .canSendMessages(false)
                .canSendOtherMessages(false)
                .canSendPolls(false)
                .build();
        RestrictChatMember restrictChatMember = new RestrictChatMember(chatId.toString(), userId, permissions);
        restrictChatMember.forTimePeriodDuration(Duration.ofMinutes(minutes));

        bot.execute(restrictChatMember);
    }

    protected boolean isGroupAdmin(Long userId, Long chatId, Bot bot) {
        boolean isAdmin = false;

        try {
            ChatMember chatMember = bot.execute(new GetChatMember(chatId.toString(), userId));
            if (chatMember != null
                    && (chatMember.getStatus().equals("creator")
                    || chatMember.getStatus().equals("administrator"))) {
                isAdmin = true;
            }
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

        return isAdmin;
    }
}
