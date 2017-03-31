package io.github.polysantiago.spring.rest.support;

import lombok.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;

@Getter
@EqualsAndHashCode(callSuper = false)
@ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SyntheticParametrizedTypeReference<T> extends ParameterizedTypeReference<T> {

    private final Type type;

    public static <T> SyntheticParametrizedTypeReference<T> fromResolvableType(ResolvableType resolvedType) {
        if (resolvedType.hasGenerics()) {
            return new SyntheticParametrizedTypeReference<T>(new SyntheticParametrizedType(resolvedType));
        }
        return new SyntheticParametrizedTypeReference<T>(resolvedType.getRawClass());
    }

}
