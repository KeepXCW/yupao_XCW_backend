package com.xcw.yupao.common;

/**
 * 返回工具类 对BaseResponse的封装，真正调用的类
 * @author xcw
 * 用来创建BaseResponse（通过返回类）的对象，
 * 目的：固定成功和失败时的BaseResponse返回的信息，规范返回信息，便于维护代码
 *
 */
public class ResultUtils {

    /**
     * 成功
     * @param data
     * @return
     */
    public static <T> BaseResponse<T> success(T data) {
        //对执行成功时 结果的封装
        return new BaseResponse<>(0,data,"ok");
    }

    /**
     * 失败
     *
     * @param errorCode
     * @return
     */
    public static BaseResponse error(ErrorCode errorCode) {
       // return new BaseResponse(errorCode.getCode(), errorCode.getMessage(), errorCode.getDescription());
        return new BaseResponse<>(errorCode);
    }


    /**
     * 失败
     *
     * @param code
     * @param message
     * @param description
     * @return
     *
     * 这里的code可自定义是int类型，从系统抛出的异常信息中取，不再从ErrorCode封装的错误码中取值
     */
    public static BaseResponse error(int code, String message, String description) {
        return new BaseResponse(code, null, message, description);
    }

    /**
     * 失败
     *
     * @param errorCode
     * @return
     */
    public static BaseResponse error(ErrorCode errorCode, String message, String description) {
        return new BaseResponse(errorCode.getCode(), null, message, description);
    }



    /**
     * 失败
     *
     * @param errorCode
     * @return
     */
    public static BaseResponse error(ErrorCode errorCode, String description) {
        return new BaseResponse(errorCode.getCode(), errorCode.getMessage(), description);
    }

//    public static BaseResponse error(ErrorCode errorCode , String message , String description) {
//        // return new BaseResponse(errorCode.getCode(), errorCode.getMessage(), errorCode.getDescription());
//        return new BaseResponse<>(errorCode.getCode(),message,description);
//    }

}
