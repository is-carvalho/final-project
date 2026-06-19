package org.raimundo.ecommerce.support;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Base class for integration tests with Testcontainers.
 * Provides PostgreSQL and WireMock containers for testing.
 */
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbc;

    static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ecommerce_test")
            .withUsername("ecommerce")
            .withPassword("ecommerce")
            .withExposedPorts(5432)
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2))
            .withStartupTimeout(Duration.ofSeconds(60));

    static final GenericContainer<?> wireMockContainer = new GenericContainer<>("wiremock/wiremock:3.9.1")
            .withFileSystemBind(wireMockRoot().toString(), "/home/wiremock", BindMode.READ_ONLY)
            .withExposedPorts(8080)
            .withCommand("--port", "8080", "--global-response-templating")
            .waitingFor(Wait.forHttp("/__admin/health").forPort(8080).forStatusCode(200))
            .withStartupTimeout(Duration.ofSeconds(60));

    static {
        postgreSQLContainer.start();
        wireMockContainer.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> 
            "jdbc:postgresql://localhost:" + postgreSQLContainer.getMappedPort(5432) + "/ecommerce_test");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.username", () -> "ecommerce");
        registry.add("spring.datasource.password", () -> "ecommerce");
        
        String wireMockUrl = "http://localhost:" + wireMockContainer.getMappedPort(8080);
        registry.add("external.customer-base-url", () -> wireMockUrl);
        registry.add("external.catalog-base-url", () -> wireMockUrl);
        registry.add("external.payment-base-url", () -> wireMockUrl);
        registry.add("external.notification-base-url", () -> wireMockUrl);
    }

    @BeforeEach
    void resetIntegrationState() {
        jdbc.update("delete from payment_result_events");
        jdbc.update("delete from payment_transactions");
        jdbc.update("delete from idempotency_records");
        jdbc.update("delete from order_items");
        jdbc.update("delete from orders");
    }

    private static Path wireMockRoot() {
        Path workingDirectory = Path.of(System.getProperty("user.dir"));
        Path moduleSibling = workingDirectory.resolve("../wiremock").normalize();
        if (Files.isDirectory(moduleSibling)) {
            return moduleSibling.toAbsolutePath();
        }
        return workingDirectory.resolve("wiremock").normalize().toAbsolutePath();
    }
}
