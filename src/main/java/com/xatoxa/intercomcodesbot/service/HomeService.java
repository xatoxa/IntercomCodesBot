package com.xatoxa.intercomcodesbot.service;

import com.xatoxa.intercomcodesbot.repository.EntryRepository;
import com.xatoxa.intercomcodesbot.repository.HomeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HomeService {
    @Autowired
    HomeRepository homeRepository;
}
