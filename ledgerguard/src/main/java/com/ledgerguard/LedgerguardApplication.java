package com.ledgerguard;

import org.springframework.boot.SpringApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableScheduling
public class LedgerguardApplication {

	public static void main(String[] args) {
		SpringApplication.run(LedgerguardApplication.class, args);
	}

}
