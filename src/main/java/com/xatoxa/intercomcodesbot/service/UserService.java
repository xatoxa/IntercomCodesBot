package com.xatoxa.intercomcodesbot.service;

import com.xatoxa.intercomcodesbot.entity.User;
import com.xatoxa.intercomcodesbot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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

    public List<User> findAllByAdmin(boolean isAdmin){
        return userRepository.findAllByAdmin(isAdmin);
    }

    public List<Long> findAllIdByAdmin(boolean isAdmin){
        List<Long> ids = new ArrayList<>();
        List<User> users = findAllByAdmin(isAdmin);

        for (User user:
             users) {
            ids.add(user.getChatId());
        }

        return ids;
    }

    public String findAllByAdminToString(boolean isAdmin){
        List<User> users = findAllByAdmin(isAdmin);
        StringBuilder stB = new StringBuilder();
        if (users.size() != 0) {
            for (User user :
                    users) {
                stB.append(user.toString()).append("\n");
            }
        } else stB.append("Empty");
        return stB.toString();
    }
}
