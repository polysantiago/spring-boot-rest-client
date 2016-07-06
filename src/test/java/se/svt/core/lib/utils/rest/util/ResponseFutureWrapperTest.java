package se.svt.core.lib.utils.rest.util;

//@RunWith(MockitoJUnitRunner.class)
public class ResponseFutureWrapperTest {

//    private ResponseFutureWrapper<String> responseFutureWrapper;
//    private SettableListenableFuture<ResponseEntity<String>> wrappedFuture;
//    private ResponseEntity<String> responseEntity;
//    private ListenableFutureCallback<String> callback;
//
//    @Before
//    public void setUp() {
//        wrappedFuture = new SettableListenableFuture<>();
//        responseEntity = mock(ResponseEntity.class);
//        callback = mock(ListenableFutureCallback.class);
//
//        responseFutureWrapper = new ResponseFutureWrapper(wrappedFuture);
//        responseFutureWrapper.addCallback(callback);
//    }
//
//    @Test
//    public void testOnSuccess() {
//        String responseString = "SOMESTRING";
//
//        when(responseEntity.getBody()).thenReturn(responseString);
//
//        wrappedFuture.set(responseEntity);
//
//        verify(callback).onSuccess(eq(responseString));
//        verify(responseEntity).getBody();
//    }
//
//    @Test
//    public void testOFailure() {
//        Exception exception = new Exception();
//
//        wrappedFuture.setException(exception);
//
//        verify(callback).onFailure(eq(exception));
//    }

}