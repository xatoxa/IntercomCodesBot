package com.xatoxa.intercomcodesbot.service;

import com.xatoxa.intercomcodesbot.entity.UserInvite;
import com.xatoxa.intercomcodesbot.repository.UserInviteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserInviteService {
    @Autowired
    UserInviteRepository userInviteRepository;

    public void save(UserInvite invite){
        userInviteRepository.save(invite);
    }

    public void delete(UserInvite invite){
        userInviteRepository.delete(invite);
    }

    public Long countAll(){
        return userInviteRepository.count();
    }

    public UserInvite getFirst(){
        return userInviteRepository.findTopBy();
    }

    public UserInvite findById(Long id){
        return userInviteRepository.findById(id).get();
    }
}
