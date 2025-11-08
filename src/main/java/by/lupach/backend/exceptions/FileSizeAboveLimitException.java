package by.lupach.backend.exceptions;

public class FileSizeAboveLimitException extends RuntimeException {
    public FileSizeAboveLimitException(String message) {
        super(message);
    }
}
