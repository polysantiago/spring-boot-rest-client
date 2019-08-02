package io.github.polysantiago.spring.rest.support;

import static java.util.stream.Collectors.joining;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import lombok.Getter;
import org.springframework.core.ResolvableType;

@Getter(onMethod_ = @Override)
public final class SyntheticParametrizedType implements ParameterizedType, Serializable {

  private static final long serialVersionUID = -521679299810654826L;

  private final Type rawType;
  private final Type[] actualTypeArguments;

  SyntheticParametrizedType(ResolvableType resolvedType) {
    this.rawType = resolvedType.getRawClass();
    this.actualTypeArguments = resolveGenerics(resolvedType);
  }

  private Type[] resolveGenerics(ResolvableType resolvableType) {
    if (resolvableType.hasGenerics()) {
      return Arrays.stream(resolvableType.getGenerics())
          .map(SyntheticParametrizedType::new)
          .toArray(Type[]::new);
    }
    return resolvableType.resolveGenerics();
  }

  @Override
  public Type getOwnerType() {
    return null;
  }

  @Override
  public String toString() {
    return String.format(
        "%s<%s<%s>>",
        SyntheticParametrizedType.class.getName(),
        rawType.getTypeName(),
        Arrays.stream(actualTypeArguments).map(Type::getTypeName).collect(joining(",")));
  }
}
