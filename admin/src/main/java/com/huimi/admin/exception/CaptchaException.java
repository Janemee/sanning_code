package com.huimi.admin.exception;

import org.apache.shiro.authc.AuthenticationException;

public class CaptchaException extends AuthenticationException {
    public CaptchaException() {
        super();
    }

    public CaptchaException(String message, Throwable cause) {
        super(message, cause);
    }

    public CaptchaException(String message) {
        super(message);
    }

    public CaptchaException(Throwable cause) {
        super(cause);
    }
}
