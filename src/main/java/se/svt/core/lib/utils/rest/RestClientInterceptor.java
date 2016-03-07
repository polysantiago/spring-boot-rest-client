package se.svt.core.lib.utils.rest;

import se.svt.core.lib.utils.rest.retry.RetryableException;
import se.svt.core.lib.utils.rest.support.MethodParameters;
import se.svt.core.lib.utils.rest.support.SyntheticParameterizedTypeReference;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.google.common.base.Throwables.getRootCause;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.springframework.http.MediaType.parseMediaType;

@RequiredArgsConstructor
class RestClientInterceptor implements MethodInterceptor {

    private static final String DEFAULT_PATH = "/";

    private final RestClientSpecification specification;
    private final RestTemplate restTemplate;
    private final URI serviceUrl;

    @Setter
    private boolean retryEnabled;

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Method method = methodInvocation.getMethod();

        MethodParameters methodParameters = new MethodParameters(method);

        List<MethodParameter> parameters = methodParameters.getParameters();
        Object[] arguments = methodInvocation.getArguments();

        RequestMapping request = AnnotationUtils.findAnnotation(method, RequestMapping.class);

        // Uri
        URI uri = UriComponentsBuilder.fromUri(serviceUrl)
            .path(isNotEmpty(request.value()) ? request.value()[0] : DEFAULT_PATH)
            .queryParams(getQueryParameters(parameters, arguments))
            .buildAndExpand(getPathParameters(parameters, arguments))
            .toUri();

        RequestEntity<Object> requestEntity = RequestEntity
            .method(toHttpMethod(isNotEmpty(request.method()) ? request.method()[0] : RequestMethod.GET), uri)
            .accept(produces(request.produces()))
            .contentType(contentType(request.consumes()))
            .body(body(parameters, arguments));

        try {
            if (method.getGenericReturnType() instanceof ParameterizedType) {
                return restTemplate.exchange(requestEntity, new SyntheticParameterizedTypeReference<>(method)).getBody();
            }
            return restTemplate.exchange(requestEntity, method.getReturnType()).getBody();
        } catch (HttpStatusCodeException ex) {
            HttpStatus statusCode = ex.getStatusCode();

            if (isOptional(method) && statusCode.equals(HttpStatus.NOT_FOUND)) {
                return Optional.empty();
            }

            if (retryEnabled && anyMatch(specification.getRetryableStatuses(), statusCode::equals)) {
                throw new RetryableException(ex);
            }
            throw ex;
        } catch (Exception ex) {
            if (retryEnabled && anyMatch(specification.getRetryableExceptions(), clazz -> clazz.isInstance(ex) || clazz.isInstance(getRootCause(ex)))) {
                throw new RetryableException(ex);
            }
            throw ex;
        }
    }

    private static boolean isOptional(Method method) {
        return method.getReturnType().isAssignableFrom(Optional.class);
    }

    private static <T> boolean anyMatch(T[] array, Predicate<T> predicate) {
        return Stream.of(array).anyMatch(predicate);
    }

    private MultiValueMap<String, String> getQueryParameters(List<MethodParameter> parameters, Object[] arguments) {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        if (CollectionUtils.isNotEmpty(parameters)) {
            queryParams.setAll(
                parameters.stream()
                    .filter(parameter -> parameter.hasParameterAnnotation(RequestParam.class))
                    .collect(toMap(
                        parameter -> ((RequestParam) parameter.getParameterAnnotations()[0]).value(),
                        parameter -> arguments[parameter.getParameterIndex()].toString())));
        }
        return queryParams;
    }

    private Object[] getPathParameters(List<MethodParameter> parameters, Object[] arguments) {
        if (CollectionUtils.isNotEmpty(parameters)) {
            return parameters.stream()
                .filter(parameter -> parameter.hasParameterAnnotation(PathVariable.class))
                .map(parameter -> arguments[parameter.getParameterIndex()])
                .toArray(Object[]::new);
        }
        return new Object[]{};
    }

    private static HttpMethod toHttpMethod(RequestMethod requestMethod) {
        return HttpMethod.valueOf(requestMethod.name());
    }

    private static MediaType[] produces(String[] produces) {
        return Stream.of(produces).map(MediaType::parseMediaType).toArray(MediaType[]::new);
    }

    private static MediaType contentType(String[] consumes) {
        return isNotEmpty(consumes) ? parseMediaType(consumes[0]) : MediaType.APPLICATION_OCTET_STREAM;
    }

    private static Object body(List<MethodParameter> parameters, Object[] arguments) {
        if (CollectionUtils.isNotEmpty(parameters)) {
            return parameters.stream()
                .filter(parameter -> !parameter.hasParameterAnnotations())
                .map(parameter -> arguments[parameter.getParameterIndex()])
                .findAny().orElse(null);
        }
        return null;
    }
}
