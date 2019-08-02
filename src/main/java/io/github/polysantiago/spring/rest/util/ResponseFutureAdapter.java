package io.github.polysantiago.spring.rest.util;

import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureAdapter;

public class ResponseFutureAdapter<T> extends ListenableFutureAdapter<T, ResponseEntity<T>> {

  public ResponseFutureAdapter(ListenableFuture<ResponseEntity<T>> adaptee) {
    super(adaptee);
  }

  @Override
  protected T adapt(ResponseEntity<T> adapteeResult) {
    return adapteeResult.getBody();
  }
}
