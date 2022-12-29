package com.xatoxa.intercomcodesbot.repository;

import com.xatoxa.intercomcodesbot.entity.Home;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HomeRepository extends JpaRepository<Home, Long> {
}
