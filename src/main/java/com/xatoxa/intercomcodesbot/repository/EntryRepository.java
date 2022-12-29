package com.xatoxa.intercomcodesbot.repository;

import com.xatoxa.intercomcodesbot.entity.Entry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EntryRepository extends JpaRepository<Entry, Long> {
}
