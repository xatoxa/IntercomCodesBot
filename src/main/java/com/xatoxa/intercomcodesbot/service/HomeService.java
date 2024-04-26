package com.xatoxa.intercomcodesbot.service;

import com.xatoxa.intercomcodesbot.entity.Home;
import com.xatoxa.intercomcodesbot.repository.HomeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
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
        return homeRepository.findAllBy(keyword);
    }

    public List<Home> findAll(){
        return homeRepository.findAll();
    }
}
