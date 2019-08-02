package io.github.polysantiago.spring.rest.support;

import java.lang.reflect.Type;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;

@Getter
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SyntheticParametrizedTypeReference<T> extends ParameterizedTypeReference<T> {

  private final Type type;

  public static <T> SyntheticParametrizedTypeReference<T> fromResolvableType(
      ResolvableType resolvedType) {
    if (resolvedType.hasGenerics()) {
      return new SyntheticParametrizedTypeReference<>(new SyntheticParametrizedType(resolvedType));
    }
    return new SyntheticParametrizedTypeReference<>(resolvedType.resolve());
  }
}
