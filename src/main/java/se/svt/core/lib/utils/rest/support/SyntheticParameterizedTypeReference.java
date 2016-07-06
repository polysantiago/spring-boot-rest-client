package se.svt.core.lib.utils.rest.support;

import se.svt.core.lib.utils.rest.util.ClassTypeInformation;
import se.svt.core.lib.utils.rest.util.Classes;
import se.svt.core.lib.utils.rest.util.TypeInformation;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.concurrent.ListenableFuture;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Optional;

public class SyntheticParameterizedTypeReference<T> extends ParameterizedTypeReference<T> {

    private final Type type;

    private final boolean optionalType;

    public static SyntheticParameterizedTypeReference fromMethodReturnType(Method method) {
        TypeInformation<?> typeInformation = ClassTypeInformation.fromReturnTypeOf(method);
        return fromTypeInformation(typeInformation);
    }

    public static SyntheticParameterizedTypeReference fromAsyncMethodReturnType(Method method) {
        TypeInformation<?> typeInformation = ClassTypeInformation.fromReturnTypeOf(method);
        if (!Classes.areTheSame(ListenableFuture.class, typeInformation.getType())) {
            throw new IllegalArgumentException("Illegal return type of method, should be ListenableFuture, was " +
                typeInformation.getClass());
        }
        typeInformation = typeInformation.getTypeArguments().get(0);
        return fromTypeInformation(typeInformation);
    }

    public static SyntheticParameterizedTypeReference fromTypeInformation(TypeInformation typeInformation) {
        Class<?> type = typeInformation.getRawTypeInformation().getType();
        boolean optionalType = Classes.areTheSame(Optional.class, type);
        if (isGeneric(typeInformation)) {
            return new SyntheticParameterizedTypeReference<>(new SyntheticParameterizedType(type,
                getTypeArguments(typeInformation)), optionalType);
        } else {
            return new SyntheticParameterizedTypeReference<>(type, optionalType);
        }
    }

    private SyntheticParameterizedTypeReference(Type type, boolean optionalType) {
        this.type = type;
        this.optionalType = optionalType;
    }

    private static boolean isGeneric(TypeInformation<?> typeInformation) {
        return !typeInformation.getTypeArguments().isEmpty();
    }

    private static Type[] getTypeArguments(TypeInformation<?> typeInformation) {
        return typeInformation.getTypeArguments().stream().map(TypeInformation::getType).toArray(Type[]::new);
    }

    public boolean isOptionalType() {
        return optionalType;
    }


    public Type getType() {
        return this.type;
    }

    @Override
    public boolean equals(Object obj) {
        return (this == obj || (obj instanceof SyntheticParameterizedTypeReference && this.type
            .equals(((SyntheticParameterizedTypeReference<?>) obj).type)));
    }

    @Override
    public int hashCode() {
        return this.type.hashCode();
    }

    @Override
    public String toString() {
        return "SyntheticParameterizedTypeReference<" + this.type + ">";
    }

}
