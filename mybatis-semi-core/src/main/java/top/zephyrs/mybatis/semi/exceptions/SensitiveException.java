package top.zephyrs.mybatis.semi.exceptions;

/**
 * 敏感字段加解密异常
 */
public class SensitiveException extends RuntimeException {

    public SensitiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
