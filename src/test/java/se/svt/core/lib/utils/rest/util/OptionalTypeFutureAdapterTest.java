package se.svt.core.lib.utils.rest.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Optional;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


@RunWith(MockitoJUnitRunner.class)
public class OptionalTypeFutureAdapterTest {

    private OptionalTypeFutureAdapter listenableFutureWrapper;

    private SettableListenableFuture<ResponseEntity<Optional<String>>> wrappedFuture;
    private ResponseEntity<Optional<String>> responseEntity;
    private ListenableFutureCallback<Optional<String>> callback;

    @Before
    public void setUp() {
        wrappedFuture = new SettableListenableFuture<>();
        responseEntity = mock(ResponseEntity.class);
        callback = mock(ListenableFutureCallback.class);

        listenableFutureWrapper = new OptionalTypeFutureAdapter(wrappedFuture);
        listenableFutureWrapper.addCallback(callback);
    }

    @Test
    public void testOFailure() {
        Exception exception = new Exception();

        wrappedFuture.setException(exception);

        verify(callback).onFailure(eq(exception));
    }

    @Test
    public void testNotFoundExceptionTranslatedToEmptyOptional() {
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.NOT_FOUND);

        wrappedFuture.setException(exception);

        verify(callback).onSuccess(eq(Optional.empty()));
    }

    @Test
    public void testOtherClientErrorExceptionsNotTranslatedToEmptyOptional() {
        HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);

        wrappedFuture.setException(exception);

        verify(callback).onFailure(eq(exception));
    }


}