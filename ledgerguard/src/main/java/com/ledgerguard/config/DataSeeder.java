package com.ledgerguard.config;

import com.ledgerguard.entity.Invoice;
import com.ledgerguard.repository.InvoiceRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
public class DataSeeder {

    private final InvoiceRepository invoiceRepository;

    public DataSeeder(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @PostConstruct
    public void seedDefaultInvoices() {
        if (invoiceRepository.count() == 0) {
            invoiceRepository.saveAll(List.of(
                    buildInvoice("orgA", "Acme Corp", "15000.00", "PAID"),
                    buildInvoice("orgA", "Global India", "8500.50", "PENDING"),
                    buildInvoice("orgA", "Apex Tech", "4200.00", "OVERDUE"),
                    buildInvoice("orgA", "Stark Industries", "25000.00", "PAID"),
                    buildInvoice("orgB", "Horizon Ltd", "99000.00", "PAID"),
                    buildInvoice("orgB", "Umbrella Corp", "45000.00", "PENDING")
            ));
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