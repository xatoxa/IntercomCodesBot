package com.xatoxa.intercomcodesbot.service;

import com.xatoxa.intercomcodesbot.entity.Home;
import com.xatoxa.intercomcodesbot.repository.HomeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Location;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HomeService {
    final Double DELTA = 0.0016; //можно вывести из констант, изменять её будет юзер
    
    @Autowired
    HomeRepository homeRepository;

    public Home findById(Long id){
        return homeRepository.findById(id).get();
    }

    public void save(Home home){
        homeRepository.save(home);
    }

    public void delete(Home home){
        homeRepository.delete(home);
    }

    public List<Home> findAllBy(Location location){
        //рассчёт "в лоб", но проект локальный, не вижу причин усложнять формулами и ГИС
        return homeRepository.findAllBy(
                location.getLongitude() - DELTA,
                location.getLongitude() + DELTA,
                location.getLatitude() - DELTA,
                location.getLatitude() + DELTA);
    }

    public List<Home> findAllBy(String keyword){
        keyword = prepareKeyword(keyword);

        return homeRepository.findAllBy(keyword);
    }

    public List<Home> findAll(){
        return homeRepository.findAll();
    }

    private String prepareKeyword(String keyword){
        StringBuilder stB = new StringBuilder(keyword.toLowerCase());

        replaceAll(stB, "\\p{Punct}", "");
        replaceAll(stB, ".*?\\bмп\\b.*?", "московский");
        replaceAll(stB, ".*?\\bхользы\\b.*?", "хользунова");
        replaceAll(stB, ".*?\\bхз\\b.*?", "хользунова");
        replaceAll(stB, ".*?\\bострова\\b.*?", "хользунова");
        replaceAll(stB, ".*?\\bшишки\\b.*?", "шишкова");
        replaceAll(stB, ".*?\\bармия\\b.*?", "армии");
        replaceAll(stB, ".*?\\bпоебень\\b.*?", "армии");
        replaceAll(stB, ".*?\\bлизюки\\b.*?", "лизюкова");
        replaceAll(stB, ".*?\\bлизуны\\b.*?", "лизюкова");
        replaceAll(stB, ".*?\\bжуки\\b.*?", "жукова");
        replaceAll(stB, "\\bулица\\b", "");
        replaceAll(stB, "\\bпроспект\\b", "");
        replaceAll(stB, "\\bпереулок\\b", "");
        replaceAll(stB, "\\s+", " ");

        return stB.toString().trim();
    }

    private static void replaceAll(StringBuilder sb, String find, String replace){
        Pattern p = Pattern.compile(find);
        Matcher m = p.matcher(sb);

        int startIndex = 0;

        while (m.find(startIndex)){
            sb.replace(m.start(), m.end(), replace);

            startIndex = m.start() + replace.length();
        }
    }
}
