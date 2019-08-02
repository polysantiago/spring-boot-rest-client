package io.github.polysantiago.spring.rest.support;

import static java.util.stream.Collectors.toList;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.IntStream;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;

@Getter
public class MethodParameters {

  private final List<MethodParameter> parameters;

  public MethodParameters(@NonNull Method method) {
    this.parameters =
        IntStream.range(0, method.getParameterCount())
            .mapToObj(index -> new MethodParameter(method, index))
            .peek(
                parameter ->
                    parameter.initParameterNameDiscovery(new DefaultParameterNameDiscoverer()))
            .collect(toList());
  }
}
