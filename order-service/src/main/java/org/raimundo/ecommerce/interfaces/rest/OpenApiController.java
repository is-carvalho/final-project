package org.raimundo.ecommerce.interfaces.rest;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class OpenApiController {
    private final ClassPathResource openApi = new ClassPathResource("openapi/order-service-openapi.yaml");

    @GetMapping(value = "/v3/api-docs", produces = "application/yaml")
    String apiDocs() throws IOException {
        return openApi.getContentAsString(StandardCharsets.UTF_8);
    }

    @GetMapping(value = "/openapi.yaml", produces = MediaType.TEXT_PLAIN_VALUE)
    String openApiYaml() throws IOException {
        return openApi.getContentAsString(StandardCharsets.UTF_8);
    }
}
