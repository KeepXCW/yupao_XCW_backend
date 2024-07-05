package com.xcw.yupao.model.request;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 修改用户信息请求体
 * @author xcw
 */
@Data
public class UserUpdateRequest implements Serializable {
    private static final long serialVersionUID = -5966373744720684575L;

    /**
     * 要修改的用户的id
     */
    private Long id;
    /**
     * 用户昵称
     */
    private String username;

    /**
     * 账号后面改成手机号验证码登录，
     */
    //private String userAccount;

    /**
     * 个人介绍
     */
    private String profile;


    /**
     * 用户头像
     */
    private String avatarUrl;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 密码
     */
    private String userPassword;


    /**
     * 电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;


}
