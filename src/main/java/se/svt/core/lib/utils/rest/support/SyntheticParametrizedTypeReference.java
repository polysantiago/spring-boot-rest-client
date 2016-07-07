package se.svt.core.lib.utils.rest.support;

import se.svt.core.lib.utils.rest.util.ClassTypeInformation;
import se.svt.core.lib.utils.rest.util.TypeInformation;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.core.ParameterizedTypeReference;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Getter
@EqualsAndHashCode(callSuper = false, of = "type")
@ToString(of = "type")
@RequiredArgsConstructor
public class SyntheticParametrizedTypeReference<T> extends ParameterizedTypeReference<T> {

    private final Type type;
    private final TypeInformation<?> typeInformation;

    public static <T> SyntheticParametrizedTypeReference<T> fromMethodReturnType(Method method) {
        TypeInformation<?> typeInformation = ClassTypeInformation.fromReturnTypeOf(method);
        return fromTypeInformation(typeInformation);
    }

//    public static <T> SyntheticParametrizedTypeReference<T> fromMethodReturnTypeTypeArgument(Method method) {
//        TypeInformation<?> typeInformation = ClassTypeInformation.fromReturnTypeOf(method);
//        if (!MethodUtils.returnTypeIsGeneric(method)) {
//            throw new IllegalArgumentException("Illegal return type of method, must be generic type, was " +
//                typeInformation.getClass());
//        }
//        typeInformation = typeInformation.getTypeArguments().get(0);
//        return fromTypeInformation(typeInformation);
//    }

    public <S> SyntheticParametrizedTypeReference<S> getTypeArgument(int argumentIndex) {
        if (argumentIndex > typeInformation.getTypeArguments().size()) {
            throw new IndexOutOfBoundsException(
                String.format("Argument index %d out of bound, type only has %d type argments",
                    argumentIndex, typeInformation.getTypeArguments().size()));
        }
        TypeInformation<?> typeArgument = typeInformation.getTypeArguments().get(argumentIndex);
        return fromTypeInformation(typeArgument);
    }

    private static <T> SyntheticParametrizedTypeReference<T> fromTypeInformation(TypeInformation typeInformation) {
        Class<?> type = typeInformation.getRawTypeInformation().getType();
        if (isGeneric(typeInformation)) {
            SyntheticParametrizedType parametrizedType = new SyntheticParametrizedType(type, getTypeArguments(typeInformation));
            return new SyntheticParametrizedTypeReference<>(parametrizedType, typeInformation);
        }
        return new SyntheticParametrizedTypeReference<>(type, typeInformation);
    }

    public boolean isOptionalType() {
        return Objects.equals(typeInformation.getRawTypeInformation().getType(), Optional.class);
    }

    private static boolean isGeneric(TypeInformation<?> typeInformation) {
        return isNotEmpty(typeInformation.getTypeArguments());
    }

    private static Type[] getTypeArguments(TypeInformation<?> typeInformation) {
        return typeInformation.getTypeArguments().stream().map(TypeInformation::getType).toArray(Type[]::new);
    }

}
