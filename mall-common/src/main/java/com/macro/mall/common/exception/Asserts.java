package com.macro.mall.common.exception;

import com.macro.mall.common.api.IErrorCode;

/**
 * 断言处理类，用于抛出各种API异常
 * Created by macro on 2020/2/27.
 */
public class Asserts {
    /**
     * 抛出带错误信息的API异常
     * @param message 错误信息
     */
    public static void fail(String message) {
        throw new ApiException(message);
    }

    /**
     * 抛出带错误码的API异常
     * @param errorCode 错误码
     */
    public static void fail(IErrorCode errorCode) {
        throw new ApiException(errorCode);
    }
}
