package com.ledgerguard.repository;
import com.ledgerguard.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findAllByOrgId(String orgId);
    Optional<Invoice> findByIdAndOrgId(Long id, String orgId);
    long countByOrgId(String orgId);
    long countByOrgIdAndStatus(String orgId, String status);
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Invoice i WHERE i.orgId = :orgId AND i.status = 'PAID'")
    BigDecimal sumPaidRevenueByOrgId(@Param("orgId") String orgId);
}