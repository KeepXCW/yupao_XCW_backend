package com.xcw.yupao.model.request;


import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求体
 * @author xcw
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = -2799635978045938170L;

    private String userAccount;
    private String userPassword;

}
