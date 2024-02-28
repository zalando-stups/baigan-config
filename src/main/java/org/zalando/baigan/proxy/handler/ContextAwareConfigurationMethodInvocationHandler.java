package org.zalando.baigan.proxy.handler;

import com.google.common.base.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.zalando.baigan.context.ContextProviderRetriever;
import org.zalando.baigan.model.Configuration;
import org.zalando.baigan.context.ContextProvider;
import org.zalando.baigan.repository.ConfigurationParser;
import org.zalando.baigan.repository.ConfigurationRepository;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Suppliers.memoize;
import static org.zalando.baigan.proxy.ProxyUtils.createKey;

/**
 * This class provides a concrete implementation for the Method invocation
 * handler.
 *
 * @author mchand
 */
@Service
public class ContextAwareConfigurationMethodInvocationHandler
        extends ConfigurationMethodInvocationHandler implements BeanFactoryAware {

    private final Logger LOG = LoggerFactory
            .getLogger(ConfigurationMethodInvocationHandler.class);

    private Supplier<ConfigurationRepository> configurationRepository;

    private Supplier<ConditionsProcessor> conditionsProcessor;

    private Supplier<ContextProviderRetriever> contextProviderRetriever;

    /**
     * We have to defer dependency injection and bean resolution as this bean is required by the
     * {@link org.zalando.baigan.proxy.ConfigurationServiceBeanFactory}, which is loaded very
     * early by {@link org.springframework.beans.factory.config.BeanPostProcessor}s.
     */
    @Override
    public void setBeanFactory(final BeanFactory beanFactory) throws BeansException {
        this.configurationRepository = memoize(() -> beanFactory.getBean(ConfigurationRepository.class));
        this.conditionsProcessor = memoize(() -> beanFactory.getBean(ConditionsProcessor.class));
        this.contextProviderRetriever = memoize(() -> beanFactory.getBean(ContextProviderRetriever.class));
    }

    @Override
    protected Object handleInvocation(Object proxy, Method method, Object[] args) {
        final String key = createKey(getClass(proxy), method);

        final List<ContextProvider> contextProviders = Arrays.stream(args)
                .filter(ContextProvider.class::isInstance)
                .map(ContextProvider.class::cast)
                .collect(Collectors.toList());

        final Object result = getConfig(key, contextProviders);
        if (result == null) {
            LOG.warn("No configuration found for key [{}] in configuration source, falling back to null.", key);
            return null;
        }
        if (!method.getReturnType().isInstance(result)) {
            LOG.error("Configuration repository returned object of wrong type. Expected: {}, actual: {}", method.getReturnType(), result.getClass());
            return null;
        }

        return result;
    }

    private Class<?> getClass(final Object proxy) {
        final Class<?>[] interfaces = proxy.getClass().getInterfaces();
        checkState(interfaces.length == 1, "Expected exactly one interface on proxy object.");
        return interfaces[0];
    }

    private Object getConfig(final String key, final List<ContextProvider> contextProviders) {

        final Optional<Configuration> optional = configurationRepository.get().get(key);
        if (!optional.isPresent()) {
            return null;
        }

        final Map<String, String> context = new HashMap<>();

        final ContextProviderRetriever contextProviderRetriever = this.contextProviderRetriever.get();
        for (final String param : contextProviderRetriever.getContextParameterKeys()) {
            Collection<ContextProvider> providers = contextProviderRetriever.getProvidersFor(param);
            if (CollectionUtils.isEmpty(providers)) {
                continue;
            }
            final ContextProvider provider = providers.iterator().next();
            context.put(param, provider.getContextParam(param));
        }

        if (!CollectionUtils.isEmpty(contextProviders)) {
            contextProviders.forEach(contextProvider -> {
                contextProvider
                        .getProvidedContexts()
                        .forEach(contextParam -> {
                            if(context.containsKey(contextParam)){
                                throw new RuntimeException("Cannot have more than one context provider for the same context key "+contextParam);
                            }
                            context.put(contextParam, contextProvider.getContextParam(contextParam));
                        });
            });
        }

        return conditionsProcessor.get().process(optional.get(), context);

    }
}
