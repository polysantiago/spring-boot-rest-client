package se.svt.core.lib.utils.rest.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Classes {

    public static boolean areTheSame(Class<?> classA, Class<?> classB) {
        return classA.isAssignableFrom(classB) && classB.isAssignableFrom(classA);
    }
}
