package org.zalando.baigan.e2e.filerepo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.zalando.baigan.BaiganSpringContext;
import org.zalando.baigan.annotation.ConfigurationServiceScan;
import org.zalando.baigan.e2e.configs.SomeConfigObject;
import org.zalando.baigan.e2e.configs.SomeConfiguration;
import org.zalando.baigan.repository.FileSystemConfigurationRepository;
import org.zalando.baigan.repository.RepositoryFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {FileSystemConfigurationRepositoryEnd2EndIT.RepoConfig.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FileSystemConfigurationRepositoryEnd2EndIT {

    @Autowired
    private SomeConfiguration someConfiguration;

    @Autowired
    private Path configFile;

    private static final Duration CONFIG_REFRESH_INTERVAL = Duration.ofMillis(100);
    private static final long TIME_TO_WAIT_FOR_CONFIG_REFRESH = CONFIG_REFRESH_INTERVAL.plusMillis(100).toMillis();

    @Test
    public void givenAConfigurationFile_whenConfigurationIsChanged_thenConfigurationBeanReturnsNewConfigAfterRefreshTime() throws InterruptedException, IOException {
        assertThat(someConfiguration.isThisTrue(), nullValue());
        assertThat(someConfiguration.someValue(), nullValue());
        assertThat(someConfiguration.someConfig(), nullValue());

        Files.writeString(configFile, "[{\"alias\": \"some.configuration.some.value\", \"defaultValue\": \"some value\"}]");
        Thread.sleep(TIME_TO_WAIT_FOR_CONFIG_REFRESH);
        assertThat(someConfiguration.isThisTrue(), nullValue());
        assertThat(someConfiguration.someValue(), equalTo("some value"));
        assertThat(someConfiguration.someConfig(), nullValue());

        Files.writeString(configFile, "[{ \"alias\": \"some.non.existing.config\", \"defaultValue\": \"an irrelevant value\"}," +
                "{ \"alias\": \"some.configuration.is.this.true\", \"defaultValue\": true}, " +
                "{ \"alias\": \"some.configuration.some.value\", \"defaultValue\": \"some value\"}, " +
                "{ \"alias\": \"some.configuration.some.config\", \"defaultValue\": {" +
                    "\"config_key\":\"a value\"," +
                    "\"extra_field\": \"objectMapper configured to not fail for unknown properties\"" +
                "}}, " +
                "{ \"alias\": \"some.configuration.config.list\", \"defaultValue\": [\"A\",\"B\"]}]"
        );
        Thread.sleep(TIME_TO_WAIT_FOR_CONFIG_REFRESH);
        assertThat(someConfiguration.isThisTrue(), equalTo(true));
        assertThat(someConfiguration.someValue(), equalTo("some value"));
        assertThat(someConfiguration.someConfig(), equalTo(new SomeConfigObject("a value")));
        assertThat(someConfiguration.configList(), equalTo(List.of("A", "B")));
    }

    @Test
    public void givenAConfigurationFile_whenTheFileIsUpdatedWithInvalidConfig_thenTheConfigurationIsNotUpdated() throws InterruptedException, IOException {
        Files.writeString(configFile, "[{ \"alias\": \"some.non.existing.config\", \"defaultValue\": \"an irrelevant value\"}," +
                "{ \"alias\": \"some.configuration.is.this.true\", \"defaultValue\": true}, " +
                "{ \"alias\": \"some.configuration.some.value\", \"defaultValue\": \"some value\"}]"
        );
        Thread.sleep(TIME_TO_WAIT_FOR_CONFIG_REFRESH);
        assertThat(someConfiguration.isThisTrue(), equalTo(true));
        assertThat(someConfiguration.someValue(), equalTo("some value"));

        Files.writeString(configFile, "{invalid: \"configuration]");
        Thread.sleep(200);
        assertThat(someConfiguration.isThisTrue(), equalTo(true));
        assertThat(someConfiguration.someValue(), equalTo("some value"));
    }

    @Test
    public void givenAConfigurationFile_whenConfigurationTypeIsGeneric_thenDeserializesProperly() throws IOException, InterruptedException {
        Files.writeString(configFile, "[{\"alias\": \"some.configuration.top.level.generics\",\"defaultValue\": {" +
                "\"a8a23682-1623-450b-8817-50c98827ea4e\": [{\"config_key\":\"A\"}]," +
                "\"76ced443-6555-4748-a22e-8700f3864e59\": [{\"config_key\":\"B\"}]}" +
                "}]");
        Thread.sleep(TIME_TO_WAIT_FOR_CONFIG_REFRESH);
        assertThat(someConfiguration.topLevelGenerics(), equalTo(Map.of(
                UUID.fromString("a8a23682-1623-450b-8817-50c98827ea4e"), List.of(new SomeConfigObject("A")),
                UUID.fromString("76ced443-6555-4748-a22e-8700f3864e59"), List.of(new SomeConfigObject("B"))
        )));
    }

    @ConfigurationServiceScan(basePackageClasses = SomeConfiguration.class)
    @Testcontainers
    @ComponentScan(basePackageClasses = {BaiganSpringContext.class})
    static class RepoConfig {

        @Bean
        FileSystemConfigurationRepository configurationRepository(Path configFile, RepositoryFactory repositoryFactory) {
            return repositoryFactory.fileSystemConfigurationRepository()
                    .fileName(configFile.toString())
                    .refreshInterval(CONFIG_REFRESH_INTERVAL)
                    .objectMapper(new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false))
                    .build();
        }

        @Bean("configFile")
        Path configFile() {
            try {
                final Path configFile = Files.createTempFile("config", "json");
                Files.writeString(configFile, "[]");
                return configFile;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
