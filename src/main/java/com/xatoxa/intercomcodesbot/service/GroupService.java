package com.xatoxa.intercomcodesbot.service;

import com.xatoxa.intercomcodesbot.entity.Group;
import com.xatoxa.intercomcodesbot.repository.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroupService {
    @Autowired
    GroupRepository groupRepository;

    public void save(Group group){
        groupRepository.save(group);
    }

    public Group findById(Long id){
        return groupRepository.findById(id).get();
    }

    public void delete(Group group){
        groupRepository.delete(group);
    }

    public String findAllToString(){
        StringBuilder stB = new StringBuilder();
        List<Group> groups = groupRepository.findAll();
        if (groups.size() > 0) {
            for (Group group :
                    groups) {
                stB.append(group).append("\n");
            }
        }else{
            stB.append("Empty");
        }

        return stB.toString();
    }

    public List<Group> findAll(){
        return groupRepository.findAll();
    }
}
