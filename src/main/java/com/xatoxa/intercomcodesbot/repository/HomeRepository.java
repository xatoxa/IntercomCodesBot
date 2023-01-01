package com.xatoxa.intercomcodesbot.repository;

import com.xatoxa.intercomcodesbot.entity.Home;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HomeRepository extends JpaRepository<Home, Long> {
    @Query("SELECT h FROM Home h WHERE h.lon BETWEEN :startLon AND :endLon AND h.lat BETWEEN :startLat AND :endLat")
    List<Home> findAllByLocation(
            @Param("startLon") Double startLon,
            @Param("endLon") Double endLon,
            @Param("startLat") Double startLat,
            @Param("endLat") Double endLat);
}
