package com.nagarseva.repository;

import com.nagarseva.entity.CounterMeasurePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CounterMeasureRepository extends JpaRepository<CounterMeasurePoint, Long> {
    Optional<CounterMeasurePoint> findByPointCode(String pointCode);
}
