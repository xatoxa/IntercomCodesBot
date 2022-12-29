package com.xatoxa.intercomcodesbot.repository;

import com.xatoxa.intercomcodesbot.entity.Entrance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EntranceRepository extends JpaRepository<Entrance, Long> {
}
