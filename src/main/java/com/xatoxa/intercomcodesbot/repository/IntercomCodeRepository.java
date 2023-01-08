package com.xatoxa.intercomcodesbot.repository;

import com.xatoxa.intercomcodesbot.entity.IntercomCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IntercomCodeRepository extends JpaRepository<IntercomCode, Long> {

    long countByUserId(long userId);
}
