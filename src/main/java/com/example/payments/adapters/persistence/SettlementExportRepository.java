package com.example.payments.adapters.persistence;

import com.example.payments.domain.settlement.SettlementExport;
import com.example.payments.domain.settlement.SettlementExportStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementExportRepository extends JpaRepository<SettlementExport, UUID> {

    List<SettlementExport> findByStatus(SettlementExportStatus status);

    List<SettlementExport> findByDateRangeStartGreaterThanEqualAndDateRangeEndLessThanEqual(OffsetDateTime start,
            OffsetDateTime end);
}
