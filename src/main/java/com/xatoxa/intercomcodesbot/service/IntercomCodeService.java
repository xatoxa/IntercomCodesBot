package com.xatoxa.intercomcodesbot.service;

import com.xatoxa.intercomcodesbot.repository.EntryRepository;
import com.xatoxa.intercomcodesbot.repository.IntercomCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IntercomCodeService {
    @Autowired
    IntercomCodeRepository intercomCodeRepository;

}
