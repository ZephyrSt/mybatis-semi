package top.zephyrs.mybatis.semi.exceptions;

public class MetadataException extends RuntimeException {
    public MetadataException(String message) {
        super(message);
    }

    public MetadataException(String message, Throwable cause) {
        super(message, cause);
    }
}
