package com.xatoxa.intercomcodesbot.repository;

import com.xatoxa.intercomcodesbot.entity.UserInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserInviteRepository extends JpaRepository<UserInvite, Long> {
}
