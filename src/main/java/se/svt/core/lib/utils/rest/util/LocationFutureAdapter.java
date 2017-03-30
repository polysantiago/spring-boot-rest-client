package se.svt.core.lib.utils.rest.util;

import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureAdapter;

import java.net.URI;
import java.util.concurrent.ExecutionException;

public class LocationFutureAdapter<T> extends ListenableFutureAdapter<URI, ResponseEntity<T>> {

    public LocationFutureAdapter(ListenableFuture<ResponseEntity<T>> adaptee) {
        super(adaptee);
    }

    @Override
    protected URI adapt(ResponseEntity<T> adapteeResult) throws ExecutionException {
        return adapteeResult.getHeaders().getLocation();
    }
}
