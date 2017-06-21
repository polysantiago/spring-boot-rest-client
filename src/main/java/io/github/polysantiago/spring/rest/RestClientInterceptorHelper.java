package io.github.polysantiago.spring.rest;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.springframework.http.MediaType.parseMediaType;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.github.polysantiago.spring.rest.support.MethodParameters;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.RequestEntity.BodyBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

class RestClientInterceptorHelper {

    private static final TypeDescriptor STRING_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(String.class);
    private static final String DEFAULT_PATH = "/";

    private final Method method;
    private final List<MethodParameter> methodParameters;
    private final Object[] arguments;

    @Setter
    @Accessors(fluent = true)
    private FormattingConversionService conversionService = new DefaultFormattingConversionService();

    static RestClientInterceptorHelper from(MethodInvocation methodInvocation) {
        return new RestClientInterceptorHelper(methodInvocation);
    }

    private RestClientInterceptorHelper(@NonNull MethodInvocation methodInvocation) {
        this.method = methodInvocation.getMethod();
        this.methodParameters = new MethodParameters(method).getParameters();
        this.arguments = methodInvocation.getArguments();
    }

    RequestEntity<Object> buildRequest(URI serviceUrl) {
        RequestMapping request = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);

        // Uri
        URI uri = UriComponentsBuilder.fromUri(serviceUrl)
            .path(isNotEmpty(request.value()) ? request.value()[0] : DEFAULT_PATH)
            .queryParams(getQueryParameters(methodParameters, arguments))
            .buildAndExpand(getPathParameters(methodParameters, arguments))
            .encode()
            .toUri();

        // Request
        BodyBuilder builder = RequestEntity
            .method(toHttpMethod(isNotEmpty(request.method()) ? request.method()[0] : RequestMethod.GET), uri);

        // Accept
        if (isNotEmpty(request.produces())) {
            builder.accept(produces(request.produces()));
        }

        // Content-Type
        if (isNotEmpty(request.consumes())) {
            builder.contentType(contentType(request.consumes()));
        }

        // Extra headers
        if (isNotEmpty(request.headers())) {
            requestHeaders(request.headers(), builder);
        }
        paramHeaders(methodParameters, arguments, builder);

        return builder.body(body(methodParameters, arguments));
    }

    private MultiValueMap<String, String> getQueryParameters(List<MethodParameter> parameters, Object[] arguments) {
        if (!isEmpty(parameters)) {
            return new LinkedMultiValueMap<>(parameters.stream()
                .filter(parameter -> parameter.hasParameterAnnotation(RequestParam.class))
                .collect(toMap(
                    parameter -> ((RequestParam) parameter.getParameterAnnotations()[0]).value(),
                    parameter -> {
                        Object value = arguments[parameter.getParameterIndex()];
                        if (value instanceof Collection) {
                            return ((Collection<?>) value).stream()
                                .map(element -> convertToString(TypeDescriptor.nested(parameter, 1), element))
                                .collect(toList());
                        }
                        return singletonList(convertToString(new TypeDescriptor(parameter), value));
                    })));
        }
        return new LinkedMultiValueMap<>();
    }

    private String convertToString(TypeDescriptor sourceType, Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof String) {
            return (String) value;
        } else if (conversionService != null) {
            return (String) conversionService.convert(value, sourceType, STRING_TYPE_DESCRIPTOR);
        }
        return value.toString();
    }

    private Map<String, Object> getPathParameters(List<MethodParameter> parameters, Object[] arguments) {
        if (!isEmpty(parameters)) {
            return parameters.stream()
                .filter(parameter -> parameter.hasParameterAnnotation(PathVariable.class))
                .collect(toMap(
                    parameter -> parameter.getParameterAnnotation(PathVariable.class).value(),
                    parameter -> arguments[parameter.getParameterIndex()]));
        }
        return emptyMap();
    }

    private static HttpMethod toHttpMethod(RequestMethod requestMethod) {
        return HttpMethod.valueOf(requestMethod.name());
    }

    private static MediaType[] produces(String[] produces) {
        return Stream.of(produces).map(MediaType::parseMediaType).toArray(MediaType[]::new);
    }

    private static MediaType contentType(String[] consumes) {
        return parseMediaType(consumes[0]);
    }

    private static Object body(List<MethodParameter> parameters, Object[] arguments) {
        // Get argument for first parameter annotated with RequestBody,
        // or if non found, argument for first non-annotated parameter
        if (!isEmpty(parameters)) {
            Object body = getAnnotatedBody(parameters, arguments);
            if (body != null) {
                return body;
            }
            return getNonAnnotatedParameterValue(parameters, arguments);
        }
        return null;
    }

    private static Object getAnnotatedBody(List<MethodParameter> parameters, Object[] arguments) {
        return parameters.stream()
            .filter((methodParameter) -> methodParameter.hasParameterAnnotation(RequestBody.class))
            .findFirst()
            .map(parameter -> arguments[parameter.getParameterIndex()])
            .orElse(null);
    }

    private static Object getNonAnnotatedParameterValue(List<MethodParameter> parameters, Object[] arguments) {
        return parameters.stream()
            .filter(parameter -> !parameter.hasParameterAnnotations())
            .findFirst()
            .map(parameter -> arguments[parameter.getParameterIndex()])
            .orElse(null);
    }

    private static void requestHeaders(String[] headers, BodyBuilder builder) {
        Stream.of(headers)
            .forEach(header -> builder.header(substringBefore(header, ":"), substringAfter(header, ":")));
    }

    private void paramHeaders(List<MethodParameter> parameters, Object[] arguments, BodyBuilder builder) {
        if (!isEmpty(parameters)) {
            parameters.stream()
                .filter(parameter -> parameter.hasParameterAnnotation(RequestHeader.class))
                .forEach(
                    parameter -> builder.header(
                        ((RequestHeader) parameter.getParameterAnnotations()[0]).value(),
                        convertToString(new TypeDescriptor(parameter), arguments[parameter.getParameterIndex()])));
        }
    }

}
