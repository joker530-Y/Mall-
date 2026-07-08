package com.macro.mall.common.exception;

import com.macro.mall.common.api.IErrorCode;

/**
 * 自定义API异常
 * Created by macro on 2020/2/27.
 */
public class ApiException extends RuntimeException {
    /** 错误码 */
    private IErrorCode errorCode;

    /**
     * 通过错误码构造异常
     * @param errorCode 错误码
     */
    public ApiException(IErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 通过错误信息构造异常
     * @param message 错误信息
     */
    public ApiException(String message) {
        super(message);
    }

    /**
     * 通过包装原始异常构造异常
     * @param cause 原始异常
     */
    public ApiException(Throwable cause) {
        super(cause);
    }

    /**
     * 通过错误信息和原始异常构造异常
     * @param message 错误信息
     * @param cause 原始异常
     */
    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 获取错误码
     * @return 错误码
     */
    public IErrorCode getErrorCode() {
        return errorCode;
    }
}
