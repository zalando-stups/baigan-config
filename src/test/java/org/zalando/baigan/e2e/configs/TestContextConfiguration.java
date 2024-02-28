package org.zalando.baigan.e2e.configs;

import org.zalando.baigan.annotation.BaiganConfig;
import org.zalando.baigan.e2e.filerepo.CustomContextProvider;

@BaiganConfig
public interface TestContextConfiguration {

    String someValue();

    Boolean isThisTrue(CustomContextProvider customContextProvider);

    Boolean toggleFlag(CustomContextProvider customContextProvider, CustomContextProvider secondProvider);

}