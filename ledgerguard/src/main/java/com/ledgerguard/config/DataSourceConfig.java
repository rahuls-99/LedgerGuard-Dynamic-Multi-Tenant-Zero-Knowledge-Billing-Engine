package com.ledgerguard.config;

import com.ledgerguard.multitenancy.DynamicDataSourceManager;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}")
    private String driverClassName;

    @Bean
    @Primary
    public DataSource dataSource(DynamicDataSourceManager manager) {
        HikariDataSource masterDs = new HikariDataSource();
        masterDs.setJdbcUrl(url);
        masterDs.setUsername(username);
        masterDs.setPassword(password);
        masterDs.setDriverClassName(driverClassName);
        masterDs.setPoolName("HikariPool-MASTER");

        manager.init(masterDs);
        return manager.getRoutingDataSource();
    }
}