package org.zalando.baigan.e2e.filerepo;

import org.jetbrains.annotations.NotNull;
import org.zalando.baigan.context.ContextProvider;

import java.util.Set;

public class CustomContextProvider implements ContextProvider {

    private final Set<String> PARAMS = Set.of("appdomain");

    private final String appDomain;

    public CustomContextProvider(String appDomain) {
        this.appDomain = appDomain;
    }

    @Override
    public String getContextParam(@NotNull final String name) {
        return appDomain;
    }

    @Override
    public Set<String> getProvidedContexts() {
        return PARAMS;
    }
}
