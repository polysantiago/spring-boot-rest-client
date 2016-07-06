package se.svt.core.lib.utils.rest.support;

import se.svt.core.lib.utils.rest.util.ClassTypeInformation;
import se.svt.core.lib.utils.rest.util.TypeInformation;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.concurrent.ListenableFuture;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.ObjectUtils.notEqual;

@Getter
@EqualsAndHashCode(callSuper = false, of = "type")
@ToString(of = "type")
@RequiredArgsConstructor
public class SyntheticParametrizedTypeReference<T> extends ParameterizedTypeReference<T> {

    private final Type type;
    private final boolean optionalType;

    public static <T> SyntheticParametrizedTypeReference<T> fromMethodReturnType(Method method) {
        TypeInformation<?> typeInformation = ClassTypeInformation.fromReturnTypeOf(method);
        return fromTypeInformation(typeInformation);
    }

    public static <T> SyntheticParametrizedTypeReference<T> fromAsyncMethodReturnType(Method method) {
        TypeInformation<?> typeInformation = ClassTypeInformation.fromReturnTypeOf(method);
        if (notEqual(ListenableFuture.class, typeInformation.getType())) {
            throw new IllegalArgumentException("Illegal return type of method, should be ListenableFuture, was " + typeInformation.getClass());
        }
        typeInformation = typeInformation.getTypeArguments().get(0);
        return fromTypeInformation(typeInformation);
    }

    private static <T> SyntheticParametrizedTypeReference<T> fromTypeInformation(TypeInformation typeInformation) {
        Class<?> type = typeInformation.getRawTypeInformation().getType();
        boolean optionalType = Objects.equals(Optional.class, type);
        if (isGeneric(typeInformation)) {
            SyntheticParametrizedType parametrizedType = new SyntheticParametrizedType(type, getTypeArguments(typeInformation));
            return new SyntheticParametrizedTypeReference<>(parametrizedType, optionalType);
        }
        return new SyntheticParametrizedTypeReference<>(type, optionalType);
    }

    private static boolean isGeneric(TypeInformation<?> typeInformation) {
        return isNotEmpty(typeInformation.getTypeArguments());
    }

    private static Type[] getTypeArguments(TypeInformation<?> typeInformation) {
        return typeInformation.getTypeArguments().stream().map(TypeInformation::getType).toArray(Type[]::new);
    }

}
