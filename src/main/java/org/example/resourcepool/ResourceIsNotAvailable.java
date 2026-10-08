package org.example.resourcepool;

public class ResourceIsNotAvailable extends RuntimeException {
    public ResourceIsNotAvailable(String s) {
        super(s);
    }
}
