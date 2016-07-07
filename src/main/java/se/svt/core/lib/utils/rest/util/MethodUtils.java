package se.svt.core.lib.utils.rest.util;

import lombok.experimental.UtilityClass;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Objects;

@UtilityClass
public class MethodUtils {

    public static boolean returnTypeIsGeneric(Method method) {
        return method.getGenericReturnType() instanceof ParameterizedType;
    }

    public static boolean returnTypeIs(Method method, Class<?> returnType) {
        Class<?> clazz = ClassTypeInformation.fromReturnTypeOf(method).getType();
        return Objects.equals(returnType, clazz);
    }

    public static boolean returnTypeIsAnyOf(Method method, Class<?>... returnTypes) {
        Class<?> clazz = ClassTypeInformation.fromReturnTypeOf(method).getType();
        return Arrays.stream(returnTypes).anyMatch(returnType -> Objects.equals(returnType, clazz));
    }
}
