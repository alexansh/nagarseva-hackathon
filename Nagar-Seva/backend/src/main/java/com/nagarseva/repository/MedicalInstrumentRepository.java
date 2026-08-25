package com.nagarseva.repository;

import com.nagarseva.entity.MedicalInstrument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MedicalInstrumentRepository extends JpaRepository<MedicalInstrument, Long> {
    Optional<MedicalInstrument> findBySerialNumber(String serialNumber);
}
