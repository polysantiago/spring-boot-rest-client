package se.svt.core.lib.utils.rest.support;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

public class AnnotationAttribute {

    private final Class<? extends Annotation> annotationType;
    private final String attributeName;

    /**
     * Creates a new {@link AnnotationAttribute} to the {@code value} attribute of the given {@link Annotation} type.
     *
     * @param annotationType must not be {@literal null}.
     */
    public AnnotationAttribute(Class<? extends Annotation> annotationType) {
        this(annotationType, null);
    }

    /**
     * Creates a new {@link AnnotationAttribute} for the given {@link Annotation} type and annotation attribute name.
     *
     * @param annotationType must not be {@literal null}.
     * @param attributeName  can be {@literal null}, defaults to {@code value}.
     */
    AnnotationAttribute(Class<? extends Annotation> annotationType, String attributeName) {
        Assert.notNull(annotationType);
        this.annotationType = annotationType;
        this.attributeName = attributeName;
    }

    /**
     * Returns the annotation type.
     *
     * @return the annotationType
     */
    public Class<? extends Annotation> getAnnotationType() {
        return annotationType;
    }

    /**
     * Reads the {@link Annotation} attribute's value from the given {@link MethodParameter}.
     *
     * @param parameter must not be {@literal null}.
     * @return
     */
    public Object getValueFrom(MethodParameter parameter) {
        Assert.notNull(parameter, "MethodParameter must not be null!");
        Annotation annotation = parameter.getParameterAnnotation(annotationType);
        return annotation == null ? null : getValueFrom(annotation);
    }

    /**
     * Reads the {@link Annotation} attribute's value from the given {@link AnnotatedElement}.
     *
     * @param annotatedElement must not be {@literal null}.
     * @return
     */
    public Object getValueFrom(AnnotatedElement annotatedElement) {
        Assert.notNull(annotatedElement, "Annotated element must not be null!");
        Annotation annotation = annotatedElement.getAnnotation(annotationType);
        return annotation == null ? null : getValueFrom(annotation);
    }

    /**
     * Returns the {@link Annotation} attribute's value from the given {@link Annotation}.
     *
     * @param annotation must not be {@literal null}.
     * @return
     */
    public Object getValueFrom(Annotation annotation) {
        Assert.notNull(annotation, "Annotation must not be null!");
        return AnnotationUtils.getValue(annotation, attributeName == null ? AnnotationUtils.VALUE : attributeName);
    }
}
