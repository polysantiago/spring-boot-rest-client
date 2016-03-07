package se.svt.core.lib.utils.rest;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AbstractClassTestingTypeFilter;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.springframework.util.Assert.state;
import static org.springframework.util.StringUtils.hasText;

@Setter
class RestClientsRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, BeanClassLoaderAware {

    private ClassLoader beanClassLoader;
    private ResourceLoader resourceLoader;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry beanDefinitionRegistry) {
        ClassPathScanningCandidateComponentProvider scanner = getScanner();
        scanner.setResourceLoader(this.resourceLoader);

        Set<String> basePackages = new HashSet<>();
        Map<String, Object> annotationAttributes = metadata.getAnnotationAttributes(EnableRestClients.class.getName());
        AnnotationTypeFilter annotationTypeFilter = new AnnotationTypeFilter(RestClient.class);

        final Class<?>[] clients = annotationAttributes == null ? null : (Class<?>[]) annotationAttributes.get("clients");

        if (isEmpty(clients)) {
            scanner.addIncludeFilter(annotationTypeFilter);
            basePackages.add(ClassUtils.getPackageName(metadata.getClassName()));
        } else {
            final Set<String> clientClasses = newHashSet();
            basePackages = new HashSet<>();
            for (Class<?> clazz : clients) {
                basePackages.add(ClassUtils.getPackageName(clazz));
                clientClasses.add(clazz.getCanonicalName());
            }
            AbstractClassTestingTypeFilter filter = new AbstractClassTestingTypeFilter() {
                @Override
                protected boolean match(ClassMetadata metadata) {
                    String cleaned = metadata.getClassName().replaceAll("\\$", ".");
                    return clientClasses.contains(cleaned);
                }
            };
            scanner.addIncludeFilter(new AllTypeFilter(Arrays.asList(filter, annotationTypeFilter)));
        }

        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);
            // verify annotated class is an interface
            candidateComponents.stream().filter(candidateComponent -> candidateComponent instanceof AnnotatedBeanDefinition)
                .forEach(candidateComponent -> {
                    // verify annotated class is an interface
                    AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) candidateComponent;
                    AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
                    Assert.isTrue(annotationMetadata.isInterface(), "@RestClient can only be specified on an interface");

                    Map<String, Object> attributes = annotationMetadata.getAnnotationAttributes(RestClient.class.getCanonicalName());

                    String name = getClientName(attributes);
                    registerClientConfiguration(beanDefinitionRegistry, name, attributes);
                    registerRestClient(beanDefinitionRegistry, annotationMetadata, attributes);
                });
        }
    }

    protected ClassPathScanningCandidateComponentProvider getScanner() {
        return new ClassPathScanningCandidateComponentProvider(false) {

            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                if (beanDefinition.getMetadata().isIndependent()) {
                    if (beanDefinition.getMetadata().isInterface()
                        && beanDefinition.getMetadata().getInterfaceNames().length == 1
                        && Annotation.class.getName().equals(beanDefinition.getMetadata().getInterfaceNames()[0])) {
                        try {
                            Class<?> target = ClassUtils.forName(beanDefinition.getMetadata().getClassName(), RestClientsRegistrar.this.beanClassLoader);
                            return !target.isAnnotation();
                        } catch (Exception ex) {
                            logger.error("Could not load target class: " + beanDefinition.getMetadata().getClassName(), ex);
                        }
                    }
                    return true;
                }
                return false;
            }
        };
    }

    private String getClientName(Map<String, Object> client) {
        if (client == null) {
            return null;
        }
        String value = (String) client.get("value");
        if (!hasText(value)) {
            value = (String) client.get("name");
        }
        if (hasText(value)) {
            return value;
        }
        throw new IllegalStateException("'value' must be provided in @" + RestClient.class.getSimpleName());
    }

    private void registerClientConfiguration(BeanDefinitionRegistry beanDefinitionRegistry, String name, Map<String, Object> attributes) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RestClientSpecification.class)
            .addConstructorArgValue(name)
            .addConstructorArgValue(attributes.get("retryOn"))
            .addConstructorArgValue(attributes.get("retryOnException"));
        beanDefinitionRegistry.registerBeanDefinition(name + "." + RestClientSpecification.class.getSimpleName(), builder.getBeanDefinition());
    }

    private void registerRestClient(BeanDefinitionRegistry beanDefinitionRegistry, AnnotationMetadata annotationMetadata, Map<String, Object> attributes) {
        String className = annotationMetadata.getClassName();
        BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(RestClientFactoryBean.class);

        definition.addPropertyValue("name", getServiceId(attributes));
        definition.addPropertyValue("url", getUrl(attributes));
        definition.addPropertyValue("type", className);

        if (beanDefinitionRegistry.containsBeanDefinition("retryInterceptor")) {
            definition.addPropertyReference("retryInterceptor", "retryInterceptor");
        }

        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

        BeanDefinitionHolder holder = new BeanDefinitionHolder(definition.getBeanDefinition(), className);
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, beanDefinitionRegistry);
    }

    private String getServiceId(Map<String, Object> attributes) {
        String name = (String) attributes.get("serviceId");
        if (!hasText(name)) {
            name = (String) attributes.get("name");
        }
        if (!hasText(name)) {
            name = (String) attributes.get("value");
        }
        name = resolve(name);
        if (!hasText(name)) {
            return "";
        }

        String host = null;
        try {
            host = new URI("http://" + name).getHost();
        } catch (URISyntaxException ignored) {
            // Ignored
        }
        state(host != null, "Service id not legal hostname (" + name + ")");
        return name;
    }

    private String getUrl(Map<String, Object> attributes) {
        String url = resolve((String) attributes.get("url"));
        if (hasText(url)) {
            if (!url.contains("://")) {
                url = "http://" + url;
            }
            try {
                new URL(url);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(url + " is malformed", e);
            }
        }
        return url;
    }

    private String resolve(String value) {
        if (hasText(value) && this.resourceLoader instanceof ConfigurableApplicationContext) {
            return ((ConfigurableApplicationContext) this.resourceLoader).getEnvironment().resolvePlaceholders(value);
        }
        return value;
    }

    /**
     * Helper class to create a {@link TypeFilter} that matches if all the delegates
     * match.
     */
    @RequiredArgsConstructor
    private static class AllTypeFilter implements TypeFilter {

        @NonNull
        private final List<TypeFilter> delegates;

        @Override
        public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
            for (TypeFilter filter : this.delegates) {
                if (!filter.match(metadataReader, metadataReaderFactory)) {
                    return false;
                }
            }
            return true;
        }
    }
}
