package com.xatoxa.intercomcodesbot.service;

import com.xatoxa.intercomcodesbot.entity.Home;
import com.xatoxa.intercomcodesbot.repository.HomeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Location;

import java.util.List;

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
        keyword = keyword.toLowerCase();
        keyword = keyword.replaceAll("\\p{Punct}", "");

        keyword = keyword.replaceAll(".*?\\bмп\\b.*?", "московский");
        keyword = keyword.replaceAll(".*?\\bхользы\\b.*?", "хользунова");
        keyword = keyword.replaceAll(".*?\\bшишки\\b.*?", "шишкова");
        keyword = keyword.replaceAll(".*?\\bармия\\b.*?", "армии");
        keyword = keyword.replaceAll(".*?\\bлизюки\\b.*?", "лизюкова");
        keyword = keyword.replaceAll(".*?\\bлизуны\\b.*?", "лизюкова");
        keyword = keyword.replaceAll(".*?\\bхз\\b.*?", "хользунова");

        keyword = keyword.replaceAll("\\bулица\\b", "");
        keyword = keyword.replaceAll("\\bпроспект\\b", "");
        keyword = keyword.replaceAll("\\bпереулок\\b", "");

        keyword = keyword.replaceAll("\\s+", " ");
        keyword = keyword.trim();

        return keyword;
    }
}
