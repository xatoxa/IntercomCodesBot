package com.xatoxa.intercomcodesbot.service;

import com.xatoxa.intercomcodesbot.entity.Home;
import com.xatoxa.intercomcodesbot.repository.HomeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HomeService {
    @Autowired
    HomeRepository homeRepository;

    public void save(Home home){
        homeRepository.save(home);
    }

    public void delete(Home home){
        homeRepository.delete(home);
    }
}
