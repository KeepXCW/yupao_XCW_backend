package com.xcw.yupao.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回类
 * @param <T> 因为controller返回各种类型的数据，所以data要定义成T类型
 */
@Data
public class BaseResponse<T> implements Serializable {

    /**
     * 状态码
     */
    private  int code;

    /**
     * 数据
     */
    private T data;

    /**
     * 消息
     */
    private String message;

    /**
     * 描述
     */
    private String description;

    public BaseResponse(int code, T data, String message,String description) {
        this.code = code;
        this.data = data;
        this.message = message;
        this.description = description;
    }

    /**
     * 下面代码相当于
     *public BaseResponse(int code, T data,String message) {
     *         this.code = code;
     *         this.data = data;
     *         this.message=message;
     *         this.description=“”;
     *     }
     */
    public BaseResponse(int code, T data,String message) {

        this(code, data,message,"");
    }

//    public BaseResponse(int code, T data) {
//        this.code = code;
//        this.data = data;
//
//    }

    public BaseResponse(int code, T data) {
        this(code, data, "", "");
    }

    public BaseResponse(ErrorCode errorCode){
//        this(errorCode.getCode(), null, errorCode.getMessage(), errorCode.getDescription());
        this(errorCode.getCode(), null, errorCode.getMessage(), errorCode.getDescription());
    }

}
