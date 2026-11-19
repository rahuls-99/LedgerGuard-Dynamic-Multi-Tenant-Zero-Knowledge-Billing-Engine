package com.ledgerguard.repository;

import com.ledgerguard.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {
    Optional<IdempotencyRecord> findByOrganizationIdAndIdempotencyKey(String organizationId, String idempotencyKey);
}