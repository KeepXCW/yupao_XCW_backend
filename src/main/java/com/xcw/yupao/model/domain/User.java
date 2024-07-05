package com.xcw.yupao.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 
 * @TableName user
 */
@TableName(value ="user5")
@Data
public class User implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户昵称
     */
    private String username;

    /**
     * 账号
     */
    private String userAccount;

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
     * 标签
     */
    private String tags;

    /**
     * 电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 用户状态 0-正常
     */
    private Integer userStatus;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除(逻辑删除)
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 用户类型 0-普通用户 1-管理员
     */
    private Integer userRole;

    /**
     * 星球编号
     */
    //private String planetCode;

    /***
     * 序列话id
     * @TableField(exist = false)告诉 MyBatis-Plus 框架该字段在数据库表中并不存在，
     * 即使存在MyBatis-Plus 在执行数据库操作时也会忽略它，不会尝试映射到数据库表中的任何列
     */
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}