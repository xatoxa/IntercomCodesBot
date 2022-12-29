package com.xatoxa.intercomcodesbot.service;

import com.xatoxa.intercomcodesbot.repository.EntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EntryService {
    @Autowired
    EntryRepository entryRepository;
}
