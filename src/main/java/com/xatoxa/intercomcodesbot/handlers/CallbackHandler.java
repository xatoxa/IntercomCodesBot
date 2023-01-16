package com.xatoxa.intercomcodesbot.handlers;

import com.xatoxa.intercomcodesbot.botapi.BotState;
import com.xatoxa.intercomcodesbot.cache.CodeCache;
import com.xatoxa.intercomcodesbot.entity.*;
import com.xatoxa.intercomcodesbot.service.LocaleMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;


@Component
@Slf4j
public class CallbackHandler extends Handler{
    @Override
    public void handle(Update update, LocaleMessageService msgService){
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

                for (Long adminChatId:   //сообщение всем админам о заявке
                        userService.findAllIdByAdmin(true)) {
                    sendMessage(adminChatId,
                            msgService.get("message.sendAdminInviteRequest") + "\n" +
                            msgService.get("message.invitations") + inviteService.countAll());
                }
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

                for (Long adminChatId:   //сообщение всем админам
                        userService.findAllIdByAdmin(true)) {
                    if (adminChatId.equals(userId)) continue;
                    sendMessage(adminChatId,
                            msgService.get("message.user") +
                                    user +
                                    msgService.get("message.acceptByAdmin") +
                                    userService.findById(userId));
                }

                String nextInvites = "";
                Long countInvites = inviteService.countAll();
                if (countInvites > 0)
                    nextInvites =
                            msgService.get("message.remains") +
                            countInvites +
                            msgService.get("message.continueSolveInvites");
                editMessage(chatId, messageId,
                        msgService.get("message.acceptUser") + "\n" + nextInvites);
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

                for (Long adminChatId:   //сообщение всем админам
                        userService.findAllIdByAdmin(true)) {
                    if (adminChatId.equals(userId)) continue;
                    sendMessage(adminChatId,
                            msgService.get("message.user") +
                                    user +
                                    msgService.get("message.rejectByAdmin") +
                                    userService.findById(userId));
                }

                String nextInvites = "";
                Long countInvites = inviteService.countAll();
                if (countInvites > 0)
                    nextInvites =
                            msgService.get("message.remains") +
                                    countInvites +
                                    msgService.get("message.continueSolveInvites");
                editMessage(chatId, messageId,
                        msgService.get("message.rejectUser") + "\n" + nextInvites);
            }catch (Exception e){
                log.error(e.getMessage());
                sendMessage(chatId, msgService.get("message.error") + e.getMessage());
            }
            botState = BotState.DEFAULT;
        } else{ //обработать другие кнопки
            botState = userDataCache.getUsersCurrentBotState(userId);
        }
        answer(update.getCallbackQuery().getId());
        userDataCache.setUsersCurrentBotState(userId, botState);
    }

    private void answer(String id) {
        try {
            bot.execute(new AnswerCallbackQuery(id));
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }
}
