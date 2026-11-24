package com.ledgerguard.config;
import com.ledgerguard.entity.Invoice;
import com.ledgerguard.multitenancy.TenantContext;
import com.ledgerguard.repository.InvoiceRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
@Component
public class DataSeeder {
  private final InvoiceRepository invoiceRepository;
  public DataSeeder(InvoiceRepository invoiceRepository) { this.invoiceRepository = invoiceRepository; }
  @EventListener(ApplicationReadyEvent.class)
  public void seedDefaultInvoices() {
    try {
      TenantContext.setTenantId("alpha");
      List<Invoice> existingOrgA = invoiceRepository.findAllByOrgId("orgA");
      if (!existingOrgA.isEmpty()) {
        invoiceRepository.deleteAll(existingOrgA);
      }
      invoiceRepository.saveAll(List.of(
        buildInvoice("orgA", "Acme Corp", "15000.00", "PAID"),
        buildInvoice("orgA", "Global India", "8500.50", "PENDING"),
        buildInvoice("orgA", "Apex Tech", "4200.00", "OVERDUE"),
        buildInvoice("orgA", "Stark Industries", "25000.00", "PAID")
      ));
      invoiceRepository.findById(5L).ifPresent(inv -> {
        inv.setOrgId("orgB");
        invoiceRepository.save(inv);
      });
      if (invoiceRepository.findById(5L).isEmpty() && invoiceRepository.findAllByOrgId("orgB").isEmpty()) {
        invoiceRepository.save(buildInvoice("orgB", "Horizon Ltd", "99000.00", "PAID"));
      }
    } catch (Exception e) {
      System.err.println("DataSeeder error: " + e.getMessage());
    } finally {
      TenantContext.clear();
    }
  }
  private Invoice buildInvoice(String orgId, String customerName, String amount, String status) {
    Invoice invoice = new Invoice();
    invoice.setOrgId(orgId);
    invoice.setCustomerName(customerName);
    invoice.setAmount(new BigDecimal(amount));
    invoice.setStatus(status);
    invoice.setCreatedAt(Instant.now());
    return invoice;
  }
}