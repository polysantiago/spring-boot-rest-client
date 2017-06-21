package io.github.polysantiago.spring.rest.util;

import java.lang.reflect.Method;
import java.util.Arrays;
import lombok.experimental.UtilityClass;
import org.springframework.core.ResolvableType;

@UtilityClass
public class ResolvableTypeUtils {

    public static boolean typeIsAnyOf(ResolvableType resolvableType, Class<?>... classes) {
        return Arrays.stream(classes).anyMatch(clazz -> typeIs(resolvableType, clazz));
    }

    public static boolean typeIs(ResolvableType resolvableType, Class<?> clazz) {
        return ResolvableType.forClass(clazz).isAssignableFrom(resolvableType);
    }

    public static boolean returnTypeIs(Method method, Class<?> returnType) {
        return ResolvableType.forClass(returnType)
            .isAssignableFrom(ResolvableType.forMethodReturnType(method));
    }

    public static boolean returnTypeIsAnyOf(Method method, Class<?>... returnTypes) {
        return Arrays.stream(returnTypes).anyMatch(returnType -> returnTypeIs(method, returnType));
    }

}
