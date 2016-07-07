package se.svt.core.lib.utils.rest.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SettableListenableFuture;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ResponseFutureAdapterTest {

    private ResponseFutureAdapter<String> responseFutureWrapper;
    private SettableListenableFuture<ResponseEntity<String>> wrappedFuture;
    private ResponseEntity<String> responseEntity;
    private ListenableFutureCallback<String> callback;

    @Before
    public void setUp() {
        wrappedFuture = new SettableListenableFuture<>();
        responseEntity = mock(ResponseEntity.class);
        callback = mock(ListenableFutureCallback.class);

        responseFutureWrapper = new ResponseFutureAdapter(wrappedFuture);
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