package se.svt.core.lib.utils.rest.support;

import se.svt.core.lib.utils.rest.util.ClassTypeInformation;
import se.svt.core.lib.utils.rest.util.TypeInformation;

import org.springframework.core.ParameterizedTypeReference;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class SyntheticParameterizedTypeReference<T> extends ParameterizedTypeReference<T> {

    private final Type type;

    public SyntheticParameterizedTypeReference(Method method) {
        TypeInformation<?> typeInformation = ClassTypeInformation.fromReturnTypeOf(method);

        Class<?> type = typeInformation.getRawTypeInformation().getType();
        this.type = new SyntheticParameterizedType(type, getTypeArguments(typeInformation));
    }

    private Type[] getTypeArguments(TypeInformation<?> typeInformation) {
        return typeInformation.getTypeArguments().stream().map(TypeInformation::getType).toArray(Type[]::new);
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
