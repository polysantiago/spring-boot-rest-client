package se.svt.core.lib.utils.rest.support;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class MethodParameters {

    private final List<MethodParameter> parameters;

    /**
     * Creates a new {@link MethodParameters} from the given {@link Method}.
     *
     * @param method must not be {@literal null}.
     */
    public MethodParameters(Method method) {
        this(method, null);
    }

    /**
     * Creates a new {@link MethodParameters} for the given {@link Method} and {@link AnnotationAttribute}. If the latter
     * is given, method parameter names will be looked up from the annotation attribute if present.
     *
     * @param method           must not be {@literal null}.
     * @param namingAnnotation can be {@literal null}.
     */
    public MethodParameters(Method method, AnnotationAttribute namingAnnotation) {
        Assert.notNull(method);
        this.parameters = new ArrayList<>(method.getParameterTypes().length);

        for (int i = 0; i < method.getParameterTypes().length; i++) {
            MethodParameter parameter = new AnnotationNamingMethodParameter(method, i, namingAnnotation);
            parameter.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());
            parameters.add(parameter);
        }
    }

    /**
     * Returns all {@link MethodParameter}s.
     */
    public List<MethodParameter> getParameters() {
        return parameters;
    }

    /**
     * Returns the {@link MethodParameter} with the given name or {@literal null} if none found.
     *
     * @param name must not be {@literal null} or empty.
     */
    public MethodParameter getParameter(final String name) {
        Assert.hasText(name, "Parameter name must not be null!");
        return parameters
            .stream()
            .filter(parameter -> name.equals(parameter.getParameterName()))
            .findAny()
            .orElse(null);
    }

    /**
     * Returns all parameters of the given type.
     *
     * @param type must not be {@literal null}.
     */
    public List<MethodParameter> getParametersOfType(final Class<?> type) {
        Assert.notNull(type, "Type must not be null!");
        return getParameters()
            .stream()
            .filter(parameter -> parameter.getParameterType().equals(type))
            .collect(toList());
    }

    /**
     * Returns all {@link MethodParameter}s annotated with the given annotation type.
     *
     * @param annotation must not be {@literal null}.
     */
    public List<MethodParameter> getParametersWith(final Class<? extends Annotation> annotation) {
        Assert.notNull(annotation);
        return getParameters()
            .stream()
            .filter(parameter -> parameter.hasParameterAnnotation(annotation))
            .collect(toList());
    }

    /**
     * Custom {@link MethodParameter} extension that will favor the name configured in the {@link AnnotationAttribute} if
     * set over discovering it.
     */
    private static class AnnotationNamingMethodParameter extends MethodParameter {

        private final AnnotationAttribute attribute;
        private String name;

        /**
         * Creates a new {@link AnnotationNamingMethodParameter} for the given {@link Method}'s parameter with the given
         * index.
         *
         * @param method         must not be {@literal null}.
         * @param parameterIndex
         * @param attribute      can be {@literal null}
         */
        AnnotationNamingMethodParameter(Method method, int parameterIndex, AnnotationAttribute attribute) {
            super(method, parameterIndex);
            this.attribute = attribute;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.core.MethodParameter#getParameterName()
         */
        @Override
        public String getParameterName() {
            if (name != null) {
                return name;
            }

            if (attribute != null) {
                Object foundName = attribute.getValueFrom(this);
                if (foundName != null) {
                    name = foundName.toString();
                    return name;
                }
            }

            name = super.getParameterName();
            return name;
        }
    }
}
