package com.xatoxa.intercomcodesbot.service;

import com.xatoxa.intercomcodesbot.entity.Entrance;
import com.xatoxa.intercomcodesbot.entity.Home;
import com.xatoxa.intercomcodesbot.repository.EntranceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EntranceService {
    @Autowired
    EntranceRepository entranceRepository;

    public void save(Entrance entrance){
        entranceRepository.save(entrance);
    }

    public void delete(Entrance entrance){
        entranceRepository.delete(entrance);
    }
}
