package com.shivankkapoor.standbase;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Runs main() with the .env file and Spring startup both stubbed out. */
class StandbaseApplicationTest {

    private Properties original;

    @BeforeEach
    void saveProperties() {
        original = (Properties) System.getProperties().clone();
    }

    @AfterEach
    void restoreProperties() {
        System.setProperties(original);
    }

    private static void runMain(Map<String, String> env) {
        Dotenv dotenv = mock(Dotenv.class);
        when(dotenv.get(anyString())).thenAnswer(i -> env.get(i.<String>getArgument(0)));
        DotenvBuilder builder = mock(DotenvBuilder.class);
        when(builder.ignoreIfMissing()).thenReturn(builder);
        when(builder.load()).thenReturn(dotenv);

        try (MockedStatic<Dotenv> dotenvs = mockStatic(Dotenv.class);
             MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            dotenvs.when(Dotenv::configure).thenReturn(builder);
            String[] args = {};

            StandbaseApplication.main(args);

            spring.verify(() -> SpringApplication.run(StandbaseApplication.class, args));
        }
    }

    @Test
    void copiesEnvValuesToSystemProperties() {
        new StandbaseApplication();
        runMain(Map.of("DB_URL", "url", "DB_USER", "user", "DB_PASSWORD", "password", "SERVER_PORT", "1234",
                "DISCORD_WEBHOOK", "hook", "ENV", "DEV", "ALDROP_BASE_URL", "base", "ALDROP_API_KEY", "key"));

        assertThat(System.getProperty("SERVER_PORT")).isEqualTo("1234");
        assertThat(System.getProperty("ALDROP_API_KEY")).isEqualTo("key");
    }

    @Test
    void leavesOptionalValuesUnsetAndDefaultsThePort() {
        System.clearProperty("DISCORD_WEBHOOK");

        runMain(Map.of("DB_URL", "url", "DB_USER", "user", "DB_PASSWORD", "password"));

        assertThat(System.getProperty("SERVER_PORT")).isEqualTo("8080");
        assertThat(System.getProperty("DISCORD_WEBHOOK")).isNull();
    }
}
