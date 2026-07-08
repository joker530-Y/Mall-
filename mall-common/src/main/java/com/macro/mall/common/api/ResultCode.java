package com.macro.mall.common.api;

/**
 * API返回码封装类
 * Created by macro on 2019/4/19.
 */
public enum ResultCode implements IErrorCode {
    /** 操作成功 */
    SUCCESS(200, "操作成功"),
    /** 操作失败 */
    FAILED(500, "操作失败"),
    /** 参数检验失败 */
    VALIDATE_FAILED(404, "参数检验失败"),
    /** 暂未登录或token已经过期 */
    UNAUTHORIZED(401, "暂未登录或token已经过期"),
    /** 没有相关权限 */
    FORBIDDEN(403, "没有相关权限");
    /** 返回码 */
    private long code;
    /** 返回信息 */
    private String message;

    /**
     * 构造方法
     * @param code 返回码
     * @param message 返回信息
     */
    private ResultCode(long code, String message) {
        this.code = code;
        this.message = message;
    }

    public long getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
