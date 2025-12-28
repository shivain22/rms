package com.atparui.rms.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class MasterLiquibaseConfig {
    // Temporarily disabled to avoid conflicts with main Liquibase
    // The main application.yml Liquibase configuration will handle schema creation

    /*
    @Value("${spring.r2dbc.url}")
    private String r2dbcUrl;

    @Value("${spring.r2dbc.username}")
    private String username;

    @Value("${spring.r2dbc.password}")
    private String password;

    @Bean("masterDataSource")
    public DataSource masterDataSource() {
        // Convert R2DBC URL to JDBC URL
        String jdbcUrl = r2dbcUrl.replace("r2dbc:postgresql://", "jdbc:postgresql://");
        
        return DataSourceBuilder.create()
            .url(jdbcUrl)
            .username(username)
            .password(password)
            .driverClassName("org.postgresql.Driver")
            .build();
    }

    @Bean("masterLiquibase")
    public SpringLiquibase masterLiquibase(@Qualifier("masterDataSource") DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:config/liquibase/master.xml");
        liquibase.setContexts("dev,prod");
        return liquibase;
    }
    */
}
