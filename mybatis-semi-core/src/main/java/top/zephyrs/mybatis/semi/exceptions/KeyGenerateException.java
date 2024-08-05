package top.zephyrs.mybatis.semi.exceptions;

public class KeyGenerateException extends RuntimeException {

    public KeyGenerateException(String message) {
        super(message);
    }

    public KeyGenerateException(String message, Throwable cause) {
        super(message, cause);
    }

}
