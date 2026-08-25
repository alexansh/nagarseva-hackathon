package com.nagarseva.repository;

import com.nagarseva.entity.DoctorIncentiveLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DoctorIncentiveRepository extends JpaRepository<DoctorIncentiveLedger, Long> {
    Optional<DoctorIncentiveLedger> findByDoctorRegNo(String doctorRegNo);
}
