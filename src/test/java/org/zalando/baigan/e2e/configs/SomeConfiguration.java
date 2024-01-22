package org.zalando.baigan.e2e.configs;

import org.zalando.baigan.annotation.BaiganConfig;
import org.zalando.baigan.context.ContextProvider;
import org.zalando.baigan.e2e.filerepo.CustomContextProvider;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@BaiganConfig
public interface SomeConfiguration {
    SomeConfigObject someConfig();
    String someValue();
    Boolean isThisTrue();
    Boolean toggleFlag(ContextProvider customContextProvider, CustomContextProvider secondProvider);
    Map<UUID, List<SomeConfigObject>> topLevelGenerics();
    List<String> configList();
}
