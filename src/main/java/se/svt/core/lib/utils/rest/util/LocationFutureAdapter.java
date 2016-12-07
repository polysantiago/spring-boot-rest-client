package se.svt.core.lib.utils.rest.util;

import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureAdapter;

import java.net.URI;
import java.util.concurrent.ExecutionException;

public class LocationFutureAdapter extends ListenableFutureAdapter<URI, ResponseEntity<Object>> {

    public LocationFutureAdapter(ListenableFuture<ResponseEntity<Object>> adaptee) {
        super(adaptee);
    }

    @Override
    protected URI adapt(ResponseEntity<Object> adapteeResult) throws ExecutionException {
        return adapteeResult.getHeaders().getLocation();
    }
}
