package com.xatoxa.intercomcodesbot.service;

import com.xatoxa.intercomcodesbot.entity.User;
import com.xatoxa.intercomcodesbot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class UserService {
    @Autowired
    UserRepository userRepository;

    public boolean isEnabled(Long userId){
        boolean enabled;
        try {
            enabled = userRepository.findById(userId).get().isEnabled();
        }catch (NoSuchElementException e){
            enabled = false;
        }
        return enabled;
    }

    public boolean isAdmin(Long userId){
        boolean admin;
        try {
            admin = userRepository.findById(userId).get().isAdmin();
        }catch (NoSuchElementException e){
            admin = false;
        }
        return admin;
    }

    public boolean existsById(Long userID){
        return userRepository.existsById(userID);
    }

    public User findById(Long userId){
        return userRepository.findById(userId).get();
    }

    public void save(User user){
        userRepository.save(user);
    }

    public void delete(User user){
        userRepository.delete(user);
    }
}
