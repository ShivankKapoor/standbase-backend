package com.shivankkapoor.standbase.config;

import com.shivankkapoor.standbase.support.StubHttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class AldropClientConfigTest {

    @Test
    void sendsApiKeyToTheBaseUrl() throws Exception {
        try (StubHttpServer server = new StubHttpServer(204)) {
            RestClient client = new AldropClientConfig().aldropRestClient(server.url(), "key-123");

            client.get().uri("/ping").retrieve().toBodilessEntity();

            assertThat(server.requests).singleElement().satisfies(r -> {
                assertThat(r.path()).isEqualTo("/ping");
                assertThat(r.authorization()).isEqualTo("Bearer key-123");
            });
        }
    }
}
