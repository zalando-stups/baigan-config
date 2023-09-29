package org.zalando.baigan.e2e.filerepo;

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
import org.zalando.baigan.proxy.BaiganConfigClasses;
import org.zalando.baigan.service.FileSystemConfigurationRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

    @Test
    public void givenS3Configuration_whenConfigurationIsChangedOnS3_thenConfigurationBeanReturnsNewConfigAfterRefreshTime() throws InterruptedException, IOException {
        assertThat(someConfiguration.someConfig(), nullValue());
        assertThat(someConfiguration.isThisTrue(), nullValue());
        assertThat(someConfiguration.someValue(), nullValue());

        Files.writeString(configFile, "[{\"alias\": \"some.configuration.some.value\", \"defaultValue\": \"some value\"}]");
        Thread.sleep(1100);
        assertThat(someConfiguration.someConfig(), nullValue());
        assertThat(someConfiguration.isThisTrue(), nullValue());
        assertThat(someConfiguration.someValue(), equalTo("some value"));

        Files.writeString(configFile, "[{ \"alias\": \"some.configuration.some.config\", \"defaultValue\": {\"config_key\":\"a value\"}}," +
                "{ \"alias\": \"some.non.existing.config\", \"defaultValue\": {\"other_config_key\":\"other value\"}}," +
                "{ \"alias\": \"some.configuration.is.this.true\", \"defaultValue\": true}, " +
                "{ \"alias\": \"some.configuration.some.value\", \"defaultValue\": \"some value\"}]"
        );
        Thread.sleep(1100);
        assertThat(someConfiguration.someConfig(), equalTo(new SomeConfigObject("a value")));
        assertThat(someConfiguration.isThisTrue(), equalTo(true));
        assertThat(someConfiguration.someValue(), equalTo("some value"));
    }

    @ConfigurationServiceScan(basePackages = "org.zalando.baigan.e2e.configs")
    @Testcontainers
    @ComponentScan(basePackageClasses = {BaiganSpringContext.class})
    static class RepoConfig {

        @Bean
        FileSystemConfigurationRepository configurationRepository(Path configFile, BaiganConfigClasses baiganConfigClasses) {
            return new FileSystemConfigurationRepository(configFile.toString(), 1, baiganConfigClasses);
        }

        @Bean("configFile")
        Path configFile() {
            try {
                return Files.createTempFile("config", "json");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}