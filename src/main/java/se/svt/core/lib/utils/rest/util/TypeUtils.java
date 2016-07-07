package se.svt.core.lib.utils.rest.util;

import se.svt.core.lib.utils.rest.support.SyntheticParametrizedTypeReference;

import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.Objects;

@UtilityClass
public class TypeUtils {

    public static boolean typeIsAnyOf(SyntheticParametrizedTypeReference<?> type, Class<?>... clazzes) {
        Class<?> rawType = type.getTypeInformation().getRawTypeInformation().getType();
        return Arrays.stream(clazzes).anyMatch(returnType -> Objects.equals(returnType, rawType));
    }

    public static boolean typeIs(SyntheticParametrizedTypeReference<?> type, Class<?> clazz) {
        Class<?> rawType = type.getTypeInformation().getRawTypeInformation().getType();
        return Objects.equals(rawType, clazz);
    }
}
