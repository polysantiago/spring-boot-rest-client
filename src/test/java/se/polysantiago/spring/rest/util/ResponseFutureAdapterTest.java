package se.polysantiago.spring.rest.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SettableListenableFuture;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ResponseFutureAdapterTest {

    @Mock
    private ResponseEntity<String> responseEntity;

    @Mock
    private ListenableFutureCallback<String> callback;

    private SettableListenableFuture<ResponseEntity<String>> wrappedFuture = new SettableListenableFuture<>();

    @Before
    public void setUp() {
        ResponseFutureAdapter<String> responseFutureWrapper = new ResponseFutureAdapter<>(wrappedFuture);
        responseFutureWrapper.addCallback(callback);
    }

    @Test
    public void testOnSuccess() {
        String responseString = "SOMESTRING";

        when(responseEntity.getBody()).thenReturn(responseString);

        wrappedFuture.set(responseEntity);

        verify(callback).onSuccess(eq(responseString));
        verify(responseEntity).getBody();
    }

    @Test
    public void testOFailure() {
        Exception exception = new Exception();

        wrappedFuture.setException(exception);

        verify(callback).onFailure(eq(exception));
    }

}