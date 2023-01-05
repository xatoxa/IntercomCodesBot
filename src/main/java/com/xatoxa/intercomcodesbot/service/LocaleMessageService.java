package com.xatoxa.intercomcodesbot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class LocaleMessageService {
    private final Locale locale;
    private final MessageSource messageSource;

    public LocaleMessageService(@Value("ru-RU") String localeTag, MessageSource messageSource){
        this.messageSource = messageSource;
        this.locale = Locale.forLanguageTag(localeTag);
    }

    public String get(String message){
        return messageSource.getMessage(message, null, this.locale);
    }

    public String get(String message, Object... args){
        return messageSource.getMessage(message, args, locale);
    }
}
