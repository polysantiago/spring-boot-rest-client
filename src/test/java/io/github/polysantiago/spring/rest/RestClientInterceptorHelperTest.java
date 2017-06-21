package io.github.polysantiago.spring.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.net.URI;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public class RestClientInterceptorHelperTest {

    private static final URI ANY_URL = URI.create("http://example.com");

    @Test
    public void testCorrectPathVariableName() throws Exception {
        Method method = RestClientInterface.class.getMethod("correctPathVariable", String.class);
        MethodInvocation invocation = new MockMethodInvocation(method, new Object[]{"id"});
        RestClientInterceptorHelper helper = RestClientInterceptorHelper.from(invocation);
        assertThat(helper.buildRequest(ANY_URL)).isNotNull();
    }

    @Test
    public void testWrongPathVariableName() throws Exception {
        Method method = RestClientInterface.class.getMethod("wrongPathVariable", String.class);
        MethodInvocation invocation = new MockMethodInvocation(method, new Object[]{"not-id"});
        RestClientInterceptorHelper helper = RestClientInterceptorHelper.from(invocation);
        assertThatIllegalArgumentException()
            .isThrownBy(() -> helper.buildRequest(ANY_URL))
            .withMessage("Map has no value for 'id'");
    }

    interface RestClientInterface {

        @GetMapping("/{id}")
        void correctPathVariable(@PathVariable("id") String id);

        @GetMapping("/{id}")
        void wrongPathVariable(@PathVariable("not-id") String notId);

    }

    @Getter
    @RequiredArgsConstructor
    private class MockMethodInvocation implements MethodInvocation {
        private final Method method;
        private final Object[] arguments;

        @Override
        public Object proceed() throws Throwable {
            return null;
        }

        @Override
        public Object getThis() {
            return null;
        }

        @Override
        public AccessibleObject getStaticPart() {
            return null;
        }
    }
}