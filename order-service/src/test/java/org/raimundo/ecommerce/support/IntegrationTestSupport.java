package org.raimundo.ecommerce.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

/**
 * Base class for integration tests with Testcontainers.
 * Provides PostgreSQL and WireMock containers for testing.
 */
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ecommerce_test")
            .withUsername("ecommerce")
            .withPassword("ecommerce")
            .withExposedPorts(5432)
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2))
            .withStartupTimeout(Duration.ofSeconds(60));

    @Container
    static GenericContainer<?> wireMockContainer = new GenericContainer<>("wiremock/wiremock:3.9.1")
            .withExposedPorts(8080)
            .withCommand(
                    "--port", "8080",
                    "--root-dir", "/var/wiremock"
            )
            .waitingFor(Wait.forHttp("/__admin/health").forPort(8080).forStatusCode(200))
            .withStartupTimeout(Duration.ofSeconds(60));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> 
            "jdbc:postgresql://localhost:" + postgreSQLContainer.getMappedPort(5432) + "/ecommerce_test");
        registry.add("spring.datasource.username", () -> "ecommerce");
        registry.add("spring.datasource.password", () -> "ecommerce");
        
        String wireMockUrl = "http://localhost:" + wireMockContainer.getMappedPort(8080);
        registry.add("external.customer-base-url", () -> wireMockUrl);
        registry.add("external.catalog-base-url", () -> wireMockUrl);
        registry.add("external.payment-base-url", () -> wireMockUrl);
        registry.add("external.notification-base-url", () -> wireMockUrl);
    }
}
