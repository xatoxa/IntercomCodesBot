package com.xatoxa.intercomcodesbot.service;

import com.xatoxa.intercomcodesbot.entity.IntercomCode;
import com.xatoxa.intercomcodesbot.repository.IntercomCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IntercomCodeService {
    @Autowired
    IntercomCodeRepository intercomCodeRepository;

    public void save(IntercomCode code){
        intercomCodeRepository.save(code);
    }

    public void delete(IntercomCode code){
        intercomCodeRepository.delete(code);
    }

    public IntercomCode findById(Long id){
        return intercomCodeRepository.findById(id).get();
    }

    public String percentOfAll(long userId) {
        return String.format("%.1f", 100 * (double)intercomCodeRepository.countByUserId(userId) / (double)intercomCodeRepository.count());
    }
}
