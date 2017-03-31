package io.github.polysantiago.spring.rest.support;

import lombok.Getter;
import org.springframework.core.ResolvableType;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
public final class SyntheticParametrizedType implements ParameterizedType, Serializable {

    private static final long serialVersionUID = -521679299810654826L;

    private final Type rawType;
    private final Type[] actualTypeArguments;

    SyntheticParametrizedType(ResolvableType resolvedType) {
        this.rawType = resolvedType.getRawClass();
        this.actualTypeArguments = resolvedType.resolveGenerics();
    }

    @Override
    public Type getOwnerType() {
        return null;
    }

    @Override
    public String toString() {
        return String.format("%s<%s<%s>>", SyntheticParametrizedType.class.getName(), rawType.getTypeName(),
            Arrays.stream(actualTypeArguments).map(Type::getTypeName).collect(Collectors.joining(",")));
    }
}
