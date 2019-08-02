package io.github.polysantiago.spring.rest.util;

import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureAdapter;

public class LocationFutureAdapter<T> extends ListenableFutureAdapter<URI, ResponseEntity<T>> {

  public LocationFutureAdapter(ListenableFuture<ResponseEntity<T>> adaptee) {
    super(adaptee);
  }

  @Override
  protected URI adapt(ResponseEntity<T> adapteeResult) {
    return adapteeResult.getHeaders().getLocation();
  }
}
