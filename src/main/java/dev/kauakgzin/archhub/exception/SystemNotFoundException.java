package dev.kauakgzin.archhub.exception;

public class SystemNotFoundException extends RuntimeException {

    public SystemNotFoundException(String id) {
        super("No system registered with id '%s'".formatted(id));
    }
}
