package se.polysantiago.spring.rest.util;

import lombok.experimental.UtilityClass;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;

@UtilityClass
public class ResolvableTypeUtils {

    public static boolean returnTypeIsGeneric(Method method) {
        return method.getGenericReturnType() instanceof ParameterizedType;
    }

    public static boolean typeIsAnyOf(ResolvableType resolvableType, Class<?>... classes) {
        return Arrays.stream(classes).anyMatch(clazz -> typeIs(resolvableType, clazz));
    }

    public static boolean typeIs(ResolvableType resolvableType, Class<?> clazz) {
        return ResolvableType.forClass(clazz).isAssignableFrom(resolvableType);
    }

    public static boolean returnTypeIs(Method method, Class<?> returnType) {
        return ResolvableType.forClass(returnType).isAssignableFrom(ResolvableType.forMethodReturnType(method));
    }

    public static boolean returnTypeIsAnyOf(Method method, Class<?>... returnTypes) {
        return Arrays.stream(returnTypes).anyMatch(returnType -> returnTypeIs(method, returnType));
    }

}
