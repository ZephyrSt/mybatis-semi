package top.zephyrs.mybatis.semi.exceptions;

public class MappedProcessorException extends RuntimeException {
    public MappedProcessorException(String message) {
        super(message);
    }

    public MappedProcessorException(String message, Throwable cause) {
        super(message, cause);
    }
}
