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
import org.zalando.baigan.e2e.configs.TestContextConfiguration;
import org.zalando.baigan.repository.FileSystemConfigurationRepository;
import org.zalando.baigan.repository.RepositoryFactory;

import java.time.Duration;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {FileSystemConfigurationContextProviderEnd2EndTest.RepoConfig.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FileSystemConfigurationContextProviderEnd2EndTest {

    @Autowired
    private TestContextConfiguration testContextConfiguration;

    private static final Duration CONFIG_REFRESH_INTERVAL = Duration.ofMillis(100);

    @Test
    public void testConfigurationsWithMultipleContextsHavingTheSameKeyShouldFail() {
        assertThrows(RuntimeException.class, () -> testContextConfiguration.toggleFlag(new CustomContextProvider("1"), new CustomContextProvider("3")));
    }

    @Test
    public void testConfigurationsWithMultipleContexts() {
        assertThat(testContextConfiguration.isThisTrue(new CustomContextProvider("1")), equalTo(true));
        assertThat(testContextConfiguration.someValue(), equalTo("some value"));
    }

    @ConfigurationServiceScan(basePackageClasses = TestContextConfiguration.class)
    @Testcontainers
    @ComponentScan(basePackageClasses = {BaiganSpringContext.class})
    static class RepoConfig {

        @Bean
        FileSystemConfigurationRepository configurationRepository(RepositoryFactory repositoryFactory) {
            return repositoryFactory.fileSystemConfigurationRepository()
                    .fileName(FileSystemConfigurationContextProviderEnd2EndTest.class.getClassLoader().getResource("test-config.json").getPath())
                    .refreshInterval(CONFIG_REFRESH_INTERVAL)
                    .objectMapper(new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false))
                    .build();
        }
    }
}
