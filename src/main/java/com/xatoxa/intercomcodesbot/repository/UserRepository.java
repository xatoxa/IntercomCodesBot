package com.xatoxa.intercomcodesbot.repository;

import com.xatoxa.intercomcodesbot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
