package com.nagarseva.repository;

import com.nagarseva.entity.CaseHandoverLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CaseHandoverLogRepository extends JpaRepository<CaseHandoverLog, Long> {
    List<CaseHandoverLog> findByProformaId(Long proformaId);
}
