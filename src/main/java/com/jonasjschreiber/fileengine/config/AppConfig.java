package com.jonasjschreiber.fileengine.config;

import com.jonasjschreiber.fileengine.constants.ApplicationConstants;
import com.jonasjschreiber.fileengine.monitoring.MonitoringTimedAspect;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.micrometer.prometheus.PrometheusRenameFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.metrics.AutoTimeProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.actuate.metrics.web.client.DefaultRestTemplateExchangeTagsProvider;
import org.springframework.boot.actuate.metrics.web.client.MetricsRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableAsync
@EnableScheduling
@RequiredArgsConstructor
public class AppConfig {

    private final Environment environment;

    @Bean
    public TaskScheduler taskScheduler() {
        return new ConcurrentTaskScheduler();
    }

    @Bean
    public RestTemplate createRestTemplate(MeterRegistry meterRegistry) {
        return new RestTemplateBuilder().customizers(
                new MetricsRestTemplateCustomizer(
                        meterRegistry,
                        new DefaultRestTemplateExchangeTagsProvider(),
                        "http.client.requests",
                        new AutoTimeProperties()))
                .build();
    }
    @Bean
    MeterRegistry meterRegistry() {
        PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        prometheusRegistry.config().meterFilter(new PrometheusRenameFilter());
        return prometheusRegistry;
    }

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags("application", ApplicationConstants.APPLICATION_NAME);
    }

    @Bean
    public JvmThreadMetrics jvmThreadMetrics(MeterRegistry meterRegistry) {
        JvmThreadMetrics jvmThreadMetrics = new JvmThreadMetrics();
        jvmThreadMetrics.bindTo(meterRegistry);
        return jvmThreadMetrics;
    }

    @Bean
    public MonitoringTimedAspect timedAspect() {
        return new MonitoringTimedAspect();
    }

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setUsername(environment.getProperty("spring.datasource.username"));
        config.setPassword(environment.getProperty("spring.datasource.password"));
        config.setDriverClassName(environment.getProperty("spring.datasource.driver-class-name"));
        config.setJdbcUrl(environment.getProperty("spring.datasource.url"));
        config.setMinimumIdle(10);
        config.setMaximumPoolSize(45);
        config.setLeakDetectionThreshold(60000L);
        config.setIdleTimeout(30000L);
        config.setConnectionTimeout(300000L);
        config.addDataSourceProperty("useServerPrepStmts", Boolean.TRUE.toString());
        config.addDataSourceProperty("cachePrepStmts", Boolean.TRUE.toString());
        config.addDataSourceProperty("prepStmtCacheSize", 250);
        config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
        config.addDataSourceProperty("rewriteBatchedStatements", Boolean.TRUE.toString());
        config.addDataSourceProperty("cacheResultSetMetadata", Boolean.TRUE.toString());
        config.addDataSourceProperty("cacheServerConfiguration", Boolean.TRUE.toString());
        config.setConnectionTestQuery("SELECT 1");
        return new HikariDataSource( config );
    }



    @Bean
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        return transactionManager;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em
                = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource());
        em.setPackagesToScan(environment.getProperty("entitymanager.packages.to.scan"));

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        em.setJpaProperties(properties);

        return em;
    }

    Properties additionalProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        return properties;
    }
}
