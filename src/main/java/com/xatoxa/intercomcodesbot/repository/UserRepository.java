package com.xatoxa.intercomcodesbot.repository;

import com.xatoxa.intercomcodesbot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findAllByAdmin(boolean admin);
}
