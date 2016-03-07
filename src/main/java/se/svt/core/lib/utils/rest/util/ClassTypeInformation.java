/*
 * Copyright 2011-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.svt.core.lib.utils.rest.util;

import org.springframework.core.GenericTypeResolver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Collections.synchronizedMap;

/**
 * {@link TypeInformation} for a plain {@link Class}.
 *
 * @author Oliver Gierke
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ClassTypeInformation<S> extends TypeDiscoverer<S> {

    public static final ClassTypeInformation<Collection> COLLECTION = new ClassTypeInformation(Collection.class);
    public static final ClassTypeInformation<List> LIST = new ClassTypeInformation(List.class);
    public static final ClassTypeInformation<Set> SET = new ClassTypeInformation(Set.class);
    public static final ClassTypeInformation<Map> MAP = new ClassTypeInformation(Map.class);
    public static final ClassTypeInformation<Object> OBJECT = new ClassTypeInformation(Object.class);

    private static final Map<Class<?>, Reference<ClassTypeInformation<?>>> CACHE = synchronizedMap(new WeakHashMap<>());

    static {
        for (ClassTypeInformation<?> info : Arrays.asList(COLLECTION, LIST, SET, MAP, OBJECT)) {
            CACHE.put(info.getType(), new WeakReference<ClassTypeInformation<?>>(info));
        }
    }

    private final Class<S> type;

    /**
     * Simple factory method to easily create new instances of {@link ClassTypeInformation}.
     *
     * @param <S>
     * @param type must not be {@literal null}.
     */
    public static <S> ClassTypeInformation<S> from(Class<S> type) {
        Assert.notNull(type, "Type must not be null!");

        Reference<ClassTypeInformation<?>> cachedReference = CACHE.get(type);
        TypeInformation<?> cachedTypeInfo = cachedReference == null ? null : cachedReference.get();

        if (cachedTypeInfo != null) {
            return (ClassTypeInformation<S>) cachedTypeInfo;
        }

        ClassTypeInformation<S> result = new ClassTypeInformation<>(type);
        CACHE.put(type, new WeakReference<>(result));
        return result;
    }

    /**
     * Creates a {@link TypeInformation} from the given method's return type.
     *
     * @param method must not be {@literal null}.
     */
    public static <S> TypeInformation<S> fromReturnTypeOf(Method method) {
        Assert.notNull(method, "Method must not be null!");
        return new ClassTypeInformation(method.getDeclaringClass()).createInfo(method.getGenericReturnType());
    }

    /**
     * Creates {@link ClassTypeInformation} for the given type.
     */
    ClassTypeInformation(Class<S> type) {
        super(ClassUtils.getUserClass(type), getTypeVariableMap(type));
        this.type = type;
    }

    /**
     * Little helper to allow us to create a generified map, actually just to satisfy the compiler.
     *
     * @param type must not be {@literal null}.
     */
    private static Map<TypeVariable<?>, Type> getTypeVariableMap(Class<?> type) {
        return getTypeVariableMap(type, new HashSet<>());
    }

    @SuppressWarnings("deprecation")
    private static Map<TypeVariable<?>, Type> getTypeVariableMap(Class<?> type, Collection<Type> visited) {
        if (visited.contains(type)) {
            return emptyMap();
        }

        visited.add(type);

        Map<TypeVariable, Type> source = GenericTypeResolver.getTypeVariableMap(type);
        Map<TypeVariable<?>, Type> map = new HashMap<>(source.size());

        source.entrySet().forEach(
            entry -> {
                Type value = entry.getValue();
                map.put(entry.getKey(), entry.getValue());

                if (value instanceof Class) {
                    map.putAll(getTypeVariableMap((Class<?>) value, visited));
                }
            }
        );

        return map;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.util.TypeDiscoverer#getType()
     */
    @Override
    public Class<S> getType() {
        return type;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.util.TypeDiscoverer#getRawTypeInformation()
     */
    @Override
    public ClassTypeInformation<?> getRawTypeInformation() {
        return this;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.util.TypeDiscoverer#isAssignableFrom(org.springframework.data.util.TypeInformation)
     */
    @Override
    public boolean isAssignableFrom(TypeInformation<?> target) {
        return getType().isAssignableFrom(target.getType());
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.util.TypeDiscoverer#specialize(org.springframework.data.util.ClassTypeInformation)
     */
    @Override
    public TypeInformation<?> specialize(ClassTypeInformation<?> type) {
        return type;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return type.getName();
    }
}
